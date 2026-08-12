package compilerxyz.eval;

import compilerxyz.gen.Binding;

import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.string.TypeStringConst;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.argrecord.ArgumentEntry;
import compilerxyz.eval.value.argrecord.ArgumentRecord;
import compilerxyz.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TSet;
import java.util.ArrayDeque;
import compilerxyz.eval.dynamic.DynException;
import compilerxyz.eval.dynamic.DynPanic;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.gen.GenBuiltinTree;
import compilerxyz.gen.Value;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectStringConst;
import compilerxyz.eval.dynamic.CodeLoc.Some;
import compilerxyz.eval.dynamic.DynCompileErrors;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.value.null_.TypeNullDynamic;

public final class Eval {

  public static final String KEY_BUILTINS = "builtins";

  private Eval() {}

  public static boolean codeUsesBinding(CodeDynamic code, BindingId targetId) {
    return switch (code.code()) {
      case JavaBytecodeStoreLoad sl -> switch (targetId) {
        case BindingId.Local local -> local.key().equals(sl.key);
      };
      case JavaBytecodeSequence s -> {
        boolean found = false;
        for (var child : s.children) {
          if (codeUsesBinding(child, targetId)) {
            found = true;
            break;
          }
        }
        yield found;
      }
      case CodeDynamic nested -> codeUsesBinding(nested, targetId);
      case JavaBytecode jbc -> false;
    };
  }

  public static Result<ConstKey, String> extractConstKey(ExprContext xx, QExprId loc, Value v) {
    var typeRes = v.valueType();
    var stringConst = GenAspectStringConst.getMaybe(xx, typeRes);
    if (stringConst.optIsSome()) {
      return Result.ok(new ConstKey.Str(stringConst.optUnwrap().aspectStringConstGet(v)));
    }
    return Result.err(
        "TODO value type [X] must expose const key aspect [LIST] but doesn't");
  }

  public static EResult<CodeValue> maybeWrapCallException(
      ExprContext xx, QExprId loc, Opt<Type> calleeExc, CodeValue callCv) {
    if (calleeExc.optIsNone()) {
      return EResult.ok(callCv);
    }
    var calleeExcType = calleeExc.optUnwrap();
    if (xx.currentExceptionType.optIsNone()) {
      return EResult.err(
          loc,
          "Cannot call a function that may throw from a function that did not declare an"
              + " exception type");
    }
    var callerExc = xx.currentExceptionType.optUnwrap();
    var erb = new Erb(loc);
    var chainDestRes = ScopeChains.excDest(xx, loc);
    if (chainDestRes.hasErrors()) return chainDestRes.reerr();
    var chainDest = chainDestRes.requireValue("maybeWrapCallException chain dest");
    // Site cast node: the target converts the callee's exception to the
    // caller's declared type before it enters the chain; none when the cast
    // is free.
    var siteKey = new JumpKey();
    var siteBlockRes =
        erb.mustEr(
            xx.target.targetCastException(xx, loc, siteKey, calleeExcType, callerExc, chainDest));
    if (siteBlockRes.optIsNone()) return EResult.errs(erb.errors());
    JumpKey dest;
    Code trailer = Code.none(loc);
    if (siteBlockRes.optUnwrap().optIsSome()) {
      var skip = new JumpKey();
      var jumpOpt = erb.mustEr(xx.target.targetJump(loc, skip));
      var landOpt = erb.mustEr(xx.target.targetLand(loc, skip));
      if (jumpOpt.optIsNone() || landOpt.optIsNone()) return EResult.errs(erb.errors());
      trailer =
          Code.concat(loc, jumpOpt.optUnwrap(), Code.concat(loc, siteBlockRes.optUnwrap().optUnwrap(), landOpt.optUnwrap()));
      dest = siteKey;
    } else {
      if (chainDest.optIsNone()) {
        return EResult.ok(callCv);
      }
      dest = chainDest.optUnwrap();
    }
    var wrappedOpt = erb.mustEr(xx.target.targetGuarded(loc, dest, callCv.code()));
    if (wrappedOpt.optIsNone()) return EResult.errs(erb.errors());
    return EResult.ok(new CodeValue(Code.concat(loc, wrappedOpt.optUnwrap(), trailer), callCv.outcome()));
  }

  /**
   * Generate the drop of a value. A drop must never throw, so it is generated
   * with no declared exception type in scope: any throw (or call to a throwing
   * function) in the drop code — including code the drop itself produces — is a
   * compile error regardless of the enclosing function's declared exception
   * type. This is the single target-agnostic entry point for dropping a value;
   * enforcing the rule here (rather than in each target's targetDrop) keeps it
   * out of the hands of target authors, since targets can be user-defined.
   */
  public static EResult<Code> dropValue(ExprContext xx, QExprId loc, CodeValue cv) {
    var dropXx = xx.withExceptionType(Opt.none());
    return dropXx.target.targetDrop(dropXx, loc, cv);
  }

  public static EResult<CodeValue> typecheckConvert(
      ExprContext xx, QExprId loc, Type destType, CodeValue source) {
    if (source.value().optIsNone()) {
      return EResult.ok(source);
    }
    var erb = new Erb(loc);
    var sourceType = source.value().optUnwrap().valueType();
    CodeValue dest;
    if (sourceType.typeCanCastTo(xx, loc, destType)) {
      var castOpt = erb.mustEr(sourceType.typeCastTo(xx, loc, source, destType));
      if (castOpt.optIsNone()) {
        return EResult.errs(erb.errors());
      }
      dest = castOpt.optUnwrap();
    } else if (destType.typeCanCastFrom(xx, loc, sourceType)) {
      var castOpt = erb.mustEr(destType.typeCastFrom(xx, loc, source));
      if (castOpt.optIsNone()) {
        return EResult.errs(erb.errors());
      }
      dest = castOpt.optUnwrap();
    } else {
      return EResult.err(
          loc,
          "Incompatible types: source "
              + sourceType.typeDesc()
              + " cannot convert to dest "
              + destType.typeDesc());
    }
    return EResult.ok(dest);
  }

  public static TMap<ConstKey, Binding> bodyBuiltinScope(ExprContext xx) {
    if (xx.vx.ix.builtinScopeRootBinding.optIsNone()) {
      throw new IllegalStateException(
          "bodyBuiltinScope called before initBuiltinScope");
    }
    var rootBinding = xx.vx.ix.builtinScopeRootBinding.optUnwrap();
    var scopeEntries = new TMap<ConstKey, Binding>();
    scopeEntries.put(new ConstKey.Str(KEY_BUILTINS), rootBinding);
    return scopeEntries;
  }

  public static EResult<TMap<ConstKey, Binding>> initBuiltinScope(
      ExprContext xx, QExprId loc, ImportStateId importState) {
    var bindingRes = GenBuiltinTree.installRoot(xx, loc, importState);
    if (bindingRes.hasErrors()) {
      return bindingRes.reerr();
    }
    var rootBinding = bindingRes.requireValue("Eval::initBuiltinScope installRoot");
    xx.vx.ix.builtinScopeRootBinding = Opt.some(rootBinding);
    var scopeEntries = new TMap<ConstKey, Binding>();
    scopeEntries.put(new ConstKey.Str(KEY_BUILTINS), rootBinding);
    return EResult.ok(scopeEntries);
  }


  public static CompileImport.EvalOutcome eval(
      CompileImportContext cix, ImportStateId importState, Expr expr) {
    var rootLoc = expr.loc();
    var vx = new EvalContext(cix);
    var rootScope = new Scope(vx);
    var target = new TargetDynamic(vx.nextLocalId);
    var xx = new ExprContext(rootScope, target, vx, importState, Opt.some(new TypeStringConst()));
    var scopeRes = initBuiltinScope(xx, rootLoc, importState);
    if (scopeRes.hasErrors()) {
      return new CompileImport.EvalOutcome.Err(scopeRes.errorsOrNull().optUnwrap());
    }
    var scopeEntries = scopeRes.requireValue("Eval::eval initBuiltinScope");
    for (var entry : scopeEntries.entrySet()) {
      rootScope.pushNamed(entry.getKey(), entry.getValue());
    }
    var binaryName = EvalRootEmitter.allocateBinaryName(xx);
    vx.evalRootInternalName = EvalRootEmitter.toInternalName(binaryName);
    var cvRes = ConstEval.constEval(xx, expr);
    if (cvRes.hasErrors()) {
      return new CompileImport.EvalOutcome.Err(cvRes.errorsOrNull().optUnwrap());
    }
    var evalRes = cvRes.requireValue("Eval::eval constEval");
    var cv = evalRes.cv();
    Opt<CodeDynamic> stage1Code;
    Opt<Value> fallbackValue;
    Type valueType;
    if (cv.value().optIsNone()) {
      var codeDyn = cv.code().codeDynamic();
      if (codeDyn == null) {
        return new CompileImport.EvalOutcome.Err(
            TList.of(
                new ExprErr(
                    rootLoc,
                    TList.of(
                        new ExprSubErr.Opaque(
                            "eval: const_eval produced no compile-time value and no dynamic code")))));
      }
      var nullType = new TypeNullDynamic();
      var seq = new JavaBytecodeSequence();
      seq.add(codeDyn);
      seq.add(compilerxyz.eval.dynamic.JavaBytecodeUtils.literalNull(rootLoc));
      stage1Code = Opt.some(new CodeDynamic(new Some(rootLoc), seq));
      fallbackValue = Opt.none();
      valueType = nullType;
    } else {
      var cvValue = cv.value().optUnwrap();
      valueType = cvValue.valueType();
      if (cv.code().codeIsNone()) {
        stage1Code = Opt.none();
        fallbackValue = Opt.some(cvValue);
      } else {
        var consumeRes = GenAspectConsumeDynamic.get(xx, rootLoc, valueType);
        if (consumeRes.hasErrors()) {
          return new CompileImport.EvalOutcome.Err(consumeRes.errorsOrNull().optUnwrap());
        }
        var consume = consumeRes.requireValue("Eval::eval consume aspect");
        var dynamicRes = consume.aspectConsumeDynamicToDynamic(xx, rootLoc, cv);
        if (dynamicRes.hasErrors()) {
          return new CompileImport.EvalOutcome.Err(dynamicRes.errorsOrNull().optUnwrap());
        }
        var dynamicCode = dynamicRes.requireValue("Eval::eval consume.toDynamic");
        stage1Code = Opt.some(dynamicCode);
        fallbackValue = Opt.none();
      }
    }
    if (stage1Code.optIsNone() && vx.definitionSets.isEmpty()) {
      if (fallbackValue.optIsNone()) {
        throw new IllegalStateException(
            "eval: no dynamic code but also no fallback value");
      }
      return new CompileImport.EvalOutcome.Ok(fallbackValue.optUnwrap(), new TList<>(), false);
    }
    var dependencyLoaders = new TList<DefinitionSetClassLoader>();

    var seenSets = new TSet<DefSetId>();
    var dss = new TList<DefinitionSet>();
    var toVisit = new ArrayDeque<DefinitionSet>();
    for (var ds : vx.definitionSets.values()) {
      toVisit.push(ds);
    }
    while (true) {

      while (!toVisit.isEmpty()) {
        var ds = toVisit.pop();
        if (!seenSets.add(ds.defSetId)) {
          continue;
        }
        var installRes = ImportInstall.installDefSetToDynamic(xx, rootLoc, ds);
        if (installRes.hasErrors()) {
          return new CompileImport.EvalOutcome.Err(installRes.errorsOrNull().optUnwrap());
        }
        var installedLoader =
            installRes.requireValue("Eval::eval installDefSetToDynamic");
        dependencyLoaders.add(installedLoader);
        for (var dep : ds.dependencies.values()) {
          if (!seenSets.contains(dep.defSetId)) {
            toVisit.push(dep);
          }
        }
        dss.add(ds);
      }
      boolean foundNew = false;
      for (var ds : vx.definitionSets.values()) {
        if (!seenSets.contains(ds.defSetId)) {
          toVisit.push(ds);
          foundNew = true;
        }
      }
      if (!foundNew) {
        break;
      }
    }

    Object rawResult;
    try {
      if (stage1Code.optIsNone()) {
        if (fallbackValue.optIsNone()) {
          throw new IllegalStateException(
              "eval: no stage1 code and no fallback value");
        }

        return new CompileImport.EvalOutcome.Ok(fallbackValue.optUnwrap(), vx.logs, false);
      }
      var emitted =
          EvalRootEmitter.emit(
              target,
              binaryName,
              stage1Code.optUnwrap(),
              vx.externals);
      if (xx.vx.ix.classLoader.optIsNone()) {
        xx.vx.ix.classLoader =
            Opt.some(new DefinitionSetClassLoader(
                xx.vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders));
      }
      var loader = xx.vx.ix.classLoader.optUnwrap();
      loader.registerClass(emitted.binaryName(), emitted.bytecode());

      var rootStaticFields = new TOrderedMap<String, Object>();
      rootStaticFields.putAll(emitted.staticFieldValues());
      rootStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
      loader.registerExternals(emitted.binaryName(), rootStaticFields);
      Class<?> rootClass;
      try {
        rootClass = Class.forName(emitted.binaryName(), true, loader);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(
            "Generated root class " + emitted.binaryName() + " failed to load", e);
      }
      try {
        var runMethod = rootClass.getDeclaredMethod("run");
        rawResult = runMethod.invoke(null);
      } catch (ReflectiveOperationException e) {
        var cause = e.getCause();

        if (cause instanceof DynPanic panic) {
          return new CompileImport.EvalOutcome.Err(
              TList.of(
                  new ExprErr(rootLoc, TList.of(new ExprSubErr.Opaque("panic: " + panic.getMessage())))));
        }

        if (cause instanceof DynCompileErrors compileErrs) {
          return new CompileImport.EvalOutcome.Err(compileErrs.errors);
        }

        if (cause instanceof DynException dynEx) {
          var rendered = renderThrownValue(dynEx.payload);
          return new CompileImport.EvalOutcome.Err(
              TList.of(
                  new ExprErr(rootLoc, TList.of(new ExprSubErr.Opaque(rendered)))));
        }

        if (cause instanceof RuntimeException re) {
          throw re;
        }
        var causeMsg = cause == null ? e.toString() : (cause.getClass().getName() + ": " + cause.getMessage());
        throw new AssertionError(
            "Generated root class "
                + emitted.binaryName()
                + " invoke failed: "
                + causeMsg,
            e);
      }
    } finally {
    }

    Value finalValue;
    if (fallbackValue.optIsSome()) {
      finalValue = fallbackValue.optUnwrap();
    } else {
      var liftRes = GenAspectLift.get(xx, rootLoc, valueType);
      if (liftRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(liftRes.errorsOrNull().optUnwrap());
      }
      var liftAspect = liftRes.requireValue("Eval::eval lift aspect");
      var liftedRes = liftAspect.aspectLiftLift(xx, rootLoc, Opt.ofNullable(rawResult));
      if (liftedRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(liftedRes.errorsOrNull().optUnwrap());
      }
      var lifted = liftedRes.requireValue("Eval::eval lift result");
      finalValue = lifted;
    }
    return new CompileImport.EvalOutcome.Ok(finalValue, vx.logs, true);
  }

  private static String renderThrownValue(Object payload) {
    return payload == null ? "null" : payload.toString();
  }
}
