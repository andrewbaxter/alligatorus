package compilerxyz.eval;

import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.string.TypeStringConst;
import compilerxyz.eval.value.any.TypeAnyConst;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.argrecord.ArgumentRecord;
import compilerxyz.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.eval.value.unique.BindingUniqueDualMode;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TSet;
import java.util.ArrayDeque;
import compilerxyz.eval.builtin.InvisibleBuiltinDispatch;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry;
import compilerxyz.gen.GenBuiltinTree;

public final class Eval {

  public static final String KEY_BUILTINS = "builtins";

  public static final String PANIC_MARKER_KEY = "__panic__";

  private Eval() {}

  public static boolean codeUsesBinding(CodeDynamic code, BindingId targetId) {
    return switch (code.code()) {
      case JavaBytecode.BindingUse b -> b.id().equals(targetId);
      case JavaBytecode.BindingSet b -> codeUsesBinding(b.value(), targetId);
      case JavaBytecode.Block b -> {
        boolean found = false;
        for (var e : b.entries()) {
          boolean hit = switch (e) {
            case JavaBytecode.BlockEntry.Code c -> codeUsesBinding(c.code(), targetId);
            case JavaBytecode.BlockEntry.Bind bb -> codeUsesBinding(bb.value(), targetId);
          };
          if (hit) {
            found = true;
            break;
          }
        }
        yield found;
      }
      case JavaBytecode.Foreach f ->
          codeUsesBinding(f.base(), targetId) || codeUsesBinding(f.body(), targetId);
      case JavaBytecode.ForeachMap f ->
          codeUsesBinding(f.base(), targetId) || codeUsesBinding(f.body(), targetId);
      case JavaBytecode.If i -> {
        for (var branch : i.conditions()) {
          if (codeUsesBinding(branch.condition(), targetId)
              || codeUsesBinding(branch.body(), targetId)) {
            yield true;
          }
        }
        yield switch (i.elseBody()) {
          case Opt.Some<CodeDynamic> els -> codeUsesBinding(els.value(), targetId);
          case Opt.None<CodeDynamic> n -> false;
        };
      }
      case JavaBytecode.Match m -> {
        if (codeUsesBinding(m.base(), targetId)) yield true;
        for (var c : m.cases()) {
          if (codeUsesBinding(c.body(), targetId)) yield true;
        }
        yield switch (m.fallback()) {
          case Opt.Some<CodeDynamic> fb -> codeUsesBinding(fb.value(), targetId);
          case Opt.None<CodeDynamic> n -> false;
        };
      }
      case JavaBytecode.LoweredSequence s -> {
        boolean found = false;
        for (var e : s.elements()) {
          boolean hit = switch (e) {
            case JavaBytecode.LoweredSequence.Bytecode b -> false;
            case JavaBytecode.LoweredSequence.Code c -> codeUsesBinding(c.code(), targetId);
            case JavaBytecode.LoweredSequence.LoadExprContext lxc -> false;
          };
          if (hit) {
            found = true;
            break;
          }
        }
        yield found;
      }
      case JavaBytecode.Try t -> {
        if (codeUsesBinding(t.body(), targetId)) yield true;
        if (t.catchClause() instanceof Opt.Some<JavaBytecode.Catch> cs) {
          if (codeUsesBinding(cs.value().body(), targetId)) yield true;
        }
        yield switch (t.finallyBlock()) {
          case Opt.Some<CodeDynamic> fb -> codeUsesBinding(fb.value(), targetId);
          case Opt.None<CodeDynamic> none -> false;
        };
      }
      case CodeDynamic nested -> codeUsesBinding(nested, targetId);
      case JavaBytecode jbc -> false;
    };
  }

  public static Result<ConstKey, String> extractConstKey(ExprContext xx, QExprId loc, Value v) {
    var typeRes = v.valueTypeOf();
    var stringConst = AspectStringConst.getMaybe(xx, typeRes);
    if (stringConst instanceof Opt.Some<AspectStringConst> some) {
      return Result.ok(new ConstKey.Str(some.value().get(v)));
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
    var initialBindOpt = erb.mustEr(AspectInitialBind.get(xx, loc, calleeExcType));
    if (initialBindOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var binding = initialBindOpt.get().initialBind(catchBinding);
    var boundCv = binding.bindingUse_(xx, loc, Code.none(loc));
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
    return EResult.ok(new CodeValue(wrappedCodeOpt.get(), callCv.value()));
  }

  public static EResult<CodeValue> typecheckConvert(
      ExprContext xx, QExprId loc, Type destType, CodeValue source) {
    if (source.value().optIsNone()) {
      return EResult.ok(source);
    }
    var erb = new Erb(loc);
    var sourceType = ((Opt.Some<Value>) source.value()).value().valueTypeOf();
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
      ExprContext xx, QExprId loc, ImportStateId importState, boolean registerDynamic) {
    var treeRes = genBuiltinTree(xx, loc, importState);
    if (treeRes.hasErrors()) {
      return treeRes.reerr();
    }
    var tree = treeRes.valueOrNull();
    if (tree == null) {
      throw new IllegalStateException("genBuiltinTree returned neither value nor errors");
    }
    if (registerDynamic) {
      walkRegisterDynamic(xx, tree);
      xx.vx.ix.builtinDynamics.putAll(
          InvisibleBuiltinDispatch.hostValues());
    }
    var bindingRes = walkBuildBinding(xx, loc, tree);
    if (bindingRes.hasErrors()) {
      return bindingRes.reerr();
    }
    var rootBinding = bindingRes.valueOrNull();
    if (rootBinding == null) {
      throw new IllegalStateException("walkBuildBinding returned neither value nor errors");
    }
    xx.vx.ix.builtinScopeRootBinding = rootBinding;
    var scopeEntries = new TMap<ConstKey, Binding>();
    scopeEntries.put(new ConstKey.Str(KEY_BUILTINS), rootBinding);
    return EResult.ok(scopeEntries);
  }

  private static EResult<BuiltinTree> genBuiltinTree(
      ExprContext xx, QExprId loc, ImportStateId importState) {
    var interfaces =
        BuiltinInterfaceRegistry.bundle(importState);
    return EResult.ok(GenBuiltinTree.build(importState, interfaces));
  }

  private static EResult<Binding> walkBuildBinding(
      ExprContext xx, QExprId loc, BuiltinTree node) {
    if (node instanceof BuiltinTree.Branch branch) {
      var fields = new TOrderedMap<ConstKey, Binding>();
      for (var child : branch.children()) {
        var childRes = walkBuildBinding(xx, loc, child.node());
        if (childRes.hasErrors()) {
          return childRes.reerr();
        }
        var childBinding = childRes.valueOrNull();
        if (childBinding == null) {
          throw new IllegalStateException("walkBuildBinding child returned neither value nor errors");
        }
        fields.put(new ConstKey.Str(child.name()), childBinding);
      }
      return EResult.ok(new ArgumentRecordBinding(new ArgumentRecord(fields)));
    }
    var leaf = ((BuiltinTree.Leaf) node).leaf();
    if (leaf instanceof BuiltinTree.BuiltinLeaf.Function fn) {
      return initialBindLeaf(xx, loc, fn.type(), fn.dynamicKey());
    }
    if (leaf instanceof BuiltinTree.BuiltinLeaf.Interface iface) {
      return initialBindLeaf(xx, loc, iface.type(), iface.dynamicKey());
    }
    if (leaf instanceof BuiltinTree.BuiltinLeaf.Constructor ctor) {
      return initialBindLeaf(xx, loc, ctor.type(), ctor.dynamicKey());
    }
    if (leaf instanceof BuiltinTree.BuiltinLeaf.InitialBind ib) {
      return initialBindLeaf(xx, loc, ib.type(), ib.dynamicKey());
    }
    if (leaf instanceof BuiltinTree.BuiltinLeaf.AspectId aspectId) {
      return EResult.ok(
          new BindingUniqueDualMode(aspectId.dynamicKey(), aspectId.unique()));
    }
    if (leaf instanceof BuiltinTree.BuiltinLeaf.AnyConst anyConst) {
      var anyConstType = new TypeAnyConst();
      var cv = new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(anyConst.type())));
      var bindRes = AspectBind.get(xx, loc, anyConstType);
      if (bindRes.hasErrors()) {
        return bindRes.reerr();
      }
      var bind = bindRes.valueOrNull();
      if (bind == null) {
        throw new IllegalStateException("AspectBind.get returned neither value nor errors");
      }
      var br = bind.bind(xx, loc, cv);
      if (br.hasErrors()) {
        return br.reerr();
      }
      var result = br.valueOrNull();
      if (result == null) {
        throw new IllegalStateException("AspectBind.bind returned neither value nor errors");
      }
      return EResult.ok(result.binding());
    }
    throw new IllegalStateException("Unhandled BuiltinLeaf variant: " + leaf.getClass());
  }

  private static EResult<Binding> initialBindLeaf(
      ExprContext xx, QExprId loc, Type type, BindingId dynamicKey) {
    var ibRes = AspectInitialBind.get(xx, loc, type);
    if (ibRes.hasErrors()) {
      return ibRes.reerr();
    }
    var ib = ibRes.valueOrNull();
    if (ib == null) {
      throw new IllegalStateException("AspectInitialBind.get returned neither value nor errors");
    }
    return EResult.ok(ib.initialBind(dynamicKey));
  }

  private static void walkRegisterDynamic(ExprContext xx, BuiltinTree node) {
    if (node instanceof BuiltinTree.Branch branch) {
      for (var child : branch.children()) {
        walkRegisterDynamic(xx, child.node());
      }
      return;
    }
    var leaf = ((BuiltinTree.Leaf) node).leaf();
    BindingId dynamicKey;
    External dynamicValue;
    switch (leaf) {
      case BuiltinTree.BuiltinLeaf.Function fn -> {
        dynamicKey = fn.dynamicKey();
        dynamicValue = fn.dynamicValue();
      }
      case BuiltinTree.BuiltinLeaf.Interface iface -> {
        dynamicKey = iface.dynamicKey();
        dynamicValue = iface.dynamicValue();
      }
      case BuiltinTree.BuiltinLeaf.AspectId aspectId -> {
        dynamicKey = aspectId.dynamicKey();
        dynamicValue = aspectId.dynamicValue();
      }
      case BuiltinTree.BuiltinLeaf.Constructor ctor -> {
        dynamicKey = ctor.dynamicKey();
        dynamicValue = ctor.dynamicValue();
      }
      case BuiltinTree.BuiltinLeaf.AnyConst ignored -> {
        return;
      }
      case BuiltinTree.BuiltinLeaf.InitialBind ignored -> {
        return;
      }
    }
    var key = ImportInstall.bindingIdDynamicKey(dynamicKey);
    xx.vx.ix.builtinDynamics.put(key, dynamicValue.externalHostValue());
  }

  public static CompileImport.EvalOutcome eval(
      CompileImportContext cix, ImportStateId importState, Expr expr) {
    var rootLoc = expr.loc();
    var vx = new EvalContext(cix);
    var rootScope = new Scope(null);
    var target = new TargetDynamic(vx.nextLocalId);
    target.registerStaticFunctionDefs(cix.staticFunctionDefs);
    var xx = new ExprContext(rootScope, target, vx, importState, new TypeStringConst());
    var registerDynamic = !cix.builtinsRegistered;
    var scopeRes = initBuiltinScope(xx, rootLoc, importState, registerDynamic);
    if (scopeRes.hasErrors()) {
      return new CompileImport.EvalOutcome.Err(scopeRes.errorsOrNull());
    }
    var scopeEntries = scopeRes.valueOrNull();
    if (scopeEntries == null) {
      throw new IllegalStateException("initBuiltinScope returned neither value nor errors");
    }
    rootScope.values.putAll(scopeEntries);
    if (registerDynamic) {
      cix.builtinsRegistered = true;
    }
    var cvRes = ConstEval.constEval(xx, expr);
    if (cvRes.hasErrors()) {
      return new CompileImport.EvalOutcome.Err(cvRes.errorsOrNull());
    }
    var cv = cvRes.valueOrNull();
    if (cv == null) {
      throw new IllegalStateException("constEval returned neither value nor errors");
    }
    if (!(cv.value() instanceof Opt.Some<Value> cvValSome)) {
      return new CompileImport.EvalOutcome.Err(
          TList.of(
              new ExprErr(
                  rootLoc,
                  TList.of(
                      new ExprSubErr.Opaque(
                          "eval: const_eval produced no compile-time value")))));
    }
    var cvValue = cvValSome.value();
    var valueType = cvValue.valueTypeOf();
    Opt<CodeDynamic> stage1Code;
    Opt<Value> fallbackValue;
    if (cv.code().isNone()) {
      stage1Code = Opt.none();
      fallbackValue = Opt.some(cvValue);
    } else {
      var consumeRes = AspectConsumeDynamic.get(xx, rootLoc, valueType);
      if (consumeRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(consumeRes.errorsOrNull());
      }
      var consume = consumeRes.valueOrNull();
      if (consume == null) {
        throw new IllegalStateException(
            "AspectConsumeDynamic.get returned neither value nor errors");
      }
      var dynamicRes = consume.toDynamic(xx, rootLoc, cv);
      if (dynamicRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(dynamicRes.errorsOrNull());
      }
      var dynamicCode = dynamicRes.valueOrNull();
      if (dynamicCode == null) {
        throw new IllegalStateException(
            "AspectConsumeDynamic.toDynamic returned neither value nor errors");
      }
      stage1Code = Opt.some(dynamicCode);
      fallbackValue = Opt.none();
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
        var installedLoader = installRes.valueOrNull();
        if (installedLoader == null) {
          throw new IllegalStateException(
              "installDefSetToDynamic returned neither value nor errors");
        }
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
      var referencedBuiltins = EvalRootEmitter.referencedBuiltinIds(rootSome.value());
      for (var scopeId : referencedBuiltins) {
        var fieldName = EvalRootEmitter.builtinFieldName(scopeId);
        if (!cix.builtinDynamics.containsKey(fieldName)) {
          return new CompileImport.EvalOutcome.Err(
              TList.of(
                  new ExprErr(
                      rootLoc,
                      TList.of(
                          new ExprSubErr.Opaque(
                              "eval: root dynamic code references builtin scope id "
                                  + scopeId
                                  + " ("
                                  + fieldName
                                  + ") with no registered host value — invisible-builtin"
                                  + " dispatch (Rust install_invisible_builtins) not yet"
                                  + " ported to the JVM target")))));
        }
      }
      var emitted =
          EvalRootEmitter.emit(
              target,
              rootSome.value(),
              vx.externals,
              cix.builtinDynamics,
              cix.staticFunctionDefs);
      var loader =
          new DefinitionSetClassLoader(
              DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders);
      loader.registerClass(emitted.binaryName(), emitted.bytecode());

      var rootStaticFields = new TOrderedMap<String, Object>();
      rootStaticFields.putAll(emitted.staticFieldValues());
      for (var defRef : EvalRootEmitter.referencedDefIds(rootSome.value())) {

        if (cix.staticFunctionDefs.contains(defRef)) {
          continue;
        }
        var fieldName = ImportInstall.bindingIdDynamicKey(defRef);
        if (rootStaticFields.containsKey(fieldName)) {
          continue;
        }
        var defSet = vx.definitionSets.getOrNull(defRef.defSet());
        if (defSet == null) {
          continue;
        }
        Object defValue = null;
        for (var func : defSet.functions) {
          if (defRef.equals(func.bindingId)) {
            defValue = func.compiledFunctionInstance;
            break;
          }
        }
        if (defValue == null) {
          outer:
          for (var obj : defSet.objects) {
            for (var method : obj.methods) {
              if (defRef.equals(method.bindingId)) {
                defValue = method.compiledFunctionInstance;
                break outer;
              }
            }
            for (var impl : obj.interfaceImpls) {
              for (var m : impl.methodImpls) {
                if (defRef.equals(m.bindingId)) {
                  defValue = m.compiledFunctionInstance;
                  break outer;
                }
              }
            }
          }
        }
        if (defValue != null) {
          rootStaticFields.put(fieldName, defValue);
        }
      }
      loader.registerExternals(emitted.binaryName(), rootStaticFields);
      Class<?> rootClass;
      try {
        rootClass = Class.forName(emitted.binaryName(), true, loader);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(
            "Generated root class " + emitted.binaryName() + " failed to load", e);
      }
      try {
        var runMethod = rootClass.getDeclaredMethod("run", ExprContext.class);
        rawResult = runMethod.invoke(null, xx);
      } catch (ReflectiveOperationException e) {
        var cause = e.getCause();

        if (cause instanceof DynRuntime.DynException dynEx) {
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
      var liftRes = AspectLift.get(xx, rootLoc, valueType);
      if (liftRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(liftRes.errorsOrNull());
      }
      var liftAspect = liftRes.valueOrNull();
      if (liftAspect == null) {
        throw new IllegalStateException("AspectLift.get returned neither value nor errors");
      }
      var liftedRes = liftAspect.liftRuntime(xx, rootLoc, rawResult);
      if (liftedRes.hasErrors()) {
        return new CompileImport.EvalOutcome.Err(liftedRes.errorsOrNull());
      }
      var lifted = liftedRes.valueOrNull();
      if (lifted == null) {
        throw new IllegalStateException(
            "AspectLift.liftRuntime returned neither value nor errors");
      }
      finalValue = lifted;
    }
    return new CompileImport.EvalOutcome.Ok(finalValue, vx.logs, true);
  }

  private static String renderThrownValue(Object payload) {
    if (payload instanceof java.util.Map<?, ?> map) {
      var marker = map.get(PANIC_MARKER_KEY);
      var isPanic = marker instanceof Boolean b && b;
      if (isPanic) {
        var msg = map.get("message");
        if (msg != null) {
          return "panic: " + msg;
        }
      }
    }
    return payload == null ? "null" : payload.toString();
  }
}
