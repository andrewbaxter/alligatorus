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

public final class Eval {

  public static final String KEY_BUILTINS = "builtins";

  private Eval() {}

  public static boolean codeUsesBinding(CodeDynamic code, BindingId targetId) {
    return switch (code.code()) {
      case compilerxyz.eval.dynamic.JavaBytecodeStoreLoad sl ->
          targetId instanceof BindingId.Local local && local.key().equals(sl.key);
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
    if (stringConst instanceof Opt.Some<AspectStringConst> some) {
      return Result.ok(new ConstKey.Str(some.value().aspectStringConstGet(v)));
    }
    return Result.err(
        "TODO value type [X] must expose const key aspect [LIST] but doesn't");
  }

  public static EResult<CodeValue> maybeWrapCallException(
      ExprContext xx, QExprId loc, Opt<Type> calleeExc, CodeValue callCv) {
    if (!(calleeExc instanceof Opt.Some<Type> calleeExcSome)) {
      return EResult.ok(callCv);
    }
    var calleeExcType = calleeExcSome.value();
    if (xx.currentExceptionType == null) {
      return EResult.err(
          loc,
          "Cannot call a function that may throw from a function that did not declare an"
              + " exception type");
    }
    var callerExc = xx.currentExceptionType;
    var erb = new Erb(loc);
    var catchBinding = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var initialBindOpt = erb.mustEr(GenAspectInitialBind.get(xx, loc, calleeExcType));
    if (initialBindOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var binding = initialBindOpt.get().aspectInitialBindInitialBind(catchBinding);
    var boundCv = binding.bindingUse(xx, loc, Code.none(loc));
    var castCvOpt = erb.mustEr(calleeExcType.typeCastTo(xx, loc, boundCv, callerExc));
    if (castCvOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var castCv = castCvOpt.get();
    if (castCv.code().isNone()) {
      return EResult.ok(callCv);
    }
    var wrappedCodeOpt =
        erb.mustEr(xx.target.targetCastException(xx, loc, callCv.code(), catchBinding, castCv));
    if (wrappedCodeOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    return EResult.ok(new CodeValue(wrappedCodeOpt.get(), callCv.outcome()));
  }

  public static EResult<CodeValue> typecheckConvert(
      ExprContext xx, QExprId loc, Type destType, CodeValue source) {
    if (source.value().optIsNone()) {
      return EResult.ok(source);
    }
    var erb = new Erb(loc);
    var sourceType = ((Opt.Some<Value>) source.value()).value().valueType();
    CodeValue dest;
    if (sourceType.typeCanCastTo(xx, loc, destType)) {
      var castOpt = erb.mustEr(sourceType.typeCastTo(xx, loc, source, destType));
      if (castOpt.isEmpty()) {
        return EResult.errs(erb.errors());
      }
      dest = castOpt.get();
    } else if (destType.typeCanCastFrom(xx, loc, sourceType)) {
      var castOpt = erb.mustEr(destType.typeCastFrom(xx, loc, source));
      if (castOpt.isEmpty()) {
        return EResult.errs(erb.errors());
      }
      dest = castOpt.get();
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
    var rootBinding = xx.vx.ix.builtinScopeRootBinding;
    if (rootBinding == null) {
      throw new IllegalStateException(
          "bodyBuiltinScope called before initBuiltinScope");
    }
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
    xx.vx.ix.builtinScopeRootBinding = rootBinding;
    var scopeEntries = new TMap<ConstKey, Binding>();
    scopeEntries.put(new ConstKey.Str(KEY_BUILTINS), rootBinding);
    return EResult.ok(scopeEntries);
  }


  public static CompileImport.EvalOutcome eval(
      CompileImportContext cix, ImportStateId importState, Expr expr) {
    var rootLoc = expr.loc();
    var vx = new EvalContext(cix);
    var rootScope = new Scope();
    var target = new TargetDynamic(vx.nextLocalId);
    var xx = new ExprContext(rootScope, target, vx, importState, new TypeStringConst());
    var scopeRes = initBuiltinScope(xx, rootLoc, importState);
    if (scopeRes.hasErrors()) {
      return new CompileImport.EvalOutcome.Err(scopeRes.errorsOrNull());
    }
    var scopeEntries = scopeRes.requireValue("Eval::eval initBuiltinScope");
    for (var entry : scopeEntries.entrySet()) {
      rootScope.pushNamed(entry.getKey(), entry.getValue());
    }
    var binaryName = EvalRootEmitter.allocateBinaryName();
    vx.evalRootInternalName = EvalRootEmitter.toInternalName(binaryName);
    var cvRes = ConstEval.constEval(xx, expr);
    if (cvRes.hasErrors()) {
      return new CompileImport.EvalOutcome.Err(cvRes.errorsOrNull());
    }
    var cv = cvRes.requireValue("Eval::eval constEval");
    Opt<CodeDynamic> stage1Code;
    Opt<Value> fallbackValue;
    Type valueType;
    if (!(cv.value() instanceof Opt.Some<Value> cvValSome)) {
      var codeDyn = cv.code().dynamic();
      if (codeDyn == null) {
        return new CompileImport.EvalOutcome.Err(
            TList.of(
                new ExprErr(
                    rootLoc,
                    TList.of(
                        new ExprSubErr.Opaque(
                            "eval: const_eval produced no compile-time value and no dynamic code")))));
      }
      var nullType = new compilerxyz.eval.value.null_.TypeNullDynamic();
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(codeDyn);
      seq.add(compilerxyz.eval.dynamic.JavaBytecodeUtils.literalNull(rootLoc));
      stage1Code = Opt.some(new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(rootLoc), seq));
      fallbackValue = Opt.none();
      valueType = nullType;
    } else {
      var cvValue = cvValSome.value();
      valueType = cvValue.valueType();
      if (cv.code().isNone()) {
        stage1Code = Opt.none();
        fallbackValue = Opt.some(cvValue);
      } else {
        var consumeRes = GenAspectConsumeDynamic.get(xx, rootLoc, valueType);
        if (consumeRes.hasErrors()) {
          return new CompileImport.EvalOutcome.Err(consumeRes.errorsOrNull());
        }
        var consume = consumeRes.requireValue("Eval::eval consume aspect");
        var dynamicRes = consume.aspectConsumeDynamicToDynamic(xx, rootLoc, cv);
        if (dynamicRes.hasErrors()) {
          return new CompileImport.EvalOutcome.Err(dynamicRes.errorsOrNull());
        }
        var dynamicCode = dynamicRes.requireValue("Eval::eval consume.toDynamic");
        stage1Code = Opt.some(dynamicCode);
        fallbackValue = Opt.none();
      }
    }
    if (stage1Code instanceof Opt.None && vx.definitionSets.isEmpty()) {
      if (!(fallbackValue instanceof Opt.Some<Value> fb)) {
        throw new IllegalStateException(
            "eval: no dynamic code but also no fallback value");
      }
      return new CompileImport.EvalOutcome.Ok(fb.value(), new TList<>(), false);
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
          return new CompileImport.EvalOutcome.Err(installRes.errorsOrNull());
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
      if (!(stage1Code instanceof Opt.Some<CodeDynamic> rootSome)) {
        if (!(fallbackValue instanceof Opt.Some<Value> fb)) {
          throw new IllegalStateException(
              "eval: no stage1 code and no fallback value");
        }

        return new CompileImport.EvalOutcome.Ok(fb.value(), vx.logs, false);
      }
      var emitted =
          EvalRootEmitter.emit(
              target,
              binaryName,
              rootSome.value(),
              vx.externals);
      var loader =
          new DefinitionSetClassLoader(
              DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders);
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
    if (fallbackValue instanceof Opt.Some<Value> fb) {
      finalValue = fb.value();
    } else {
      var liftRes = GenAspectLift.get(xx, rootLoc, valueType);
      if (liftRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(liftRes.errorsOrNull());
      }
      var liftAspect = liftRes.requireValue("Eval::eval lift aspect");
      var liftedRes = liftAspect.aspectLiftLift(xx, rootLoc, rawResult);
      if (liftedRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(liftedRes.errorsOrNull());
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
