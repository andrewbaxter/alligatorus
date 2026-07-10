package compilerxyz.eval.value.source_function;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectRecord;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.value.argrecord.ArgumentEntry;
import compilerxyz.eval.value.argrecord.ArgumentRecord;
import compilerxyz.eval.value.argrecord.ArgumentTuple;
import compilerxyz.eval.value.argrecord.ArgumentTupleBinding;
import compilerxyz.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.gen.Binding;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import org.jspecify.annotations.Nullable;

public final class SourceFunctionData {
  public @Nullable DefSetId setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final @Nullable String name;
  public @Nullable String description;
  public @Nullable Type exceptionType;
  public @Nullable Value implExpr;
  public @Nullable Type argType;
  public @Nullable Type retType;
  public @Nullable BindingId bindingId;
  public @Nullable String compiledClassName;
  public @Nullable byte[] compiledBytecode;

  public DefSetFunctionsEmitter.@Nullable FunctionSpec functionSpec;

  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;
  public final TList<EvalContext.ExternalEntry> capturedExternals;

  public SourceFunctionData(DefSetId setId, @Nullable String name, DefinitionSet set) {
    this.setId = setId;
    this.set = set;
    this.name = name;
    this.capturedExternals = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public SourceFunctionData(@Nullable DefSetId setId, @Nullable String name) {
    this.setId = setId;
    this.set = (DefinitionSet) null;
    this.name = name;
    this.capturedExternals = new TList<>();
  }

  public void initSetReflectively(DefinitionSet set) {
    try {
      var f = SourceFunctionData.class.getDeclaredField("set");
      f.setAccessible(true);
      f.set(this, set);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "SourceFunctionData.initSetReflectively: failed to write final 'set' field", e);
    }
  }

  public EResult<Void> finish(ExprContext xx, QExprId rootLoc) {
    if (functionSpec != null || compiledClassName != null) {
      return EResult.ok(null);
    }
    var nameStr = name != null ? name : "(unnamed)";
    var implExprValue = implExpr;
    if (implExprValue == null) {
      return EResult.err(
          rootLoc,
          "Definition set function '" + nameStr + "' is missing an implementation");
    }
    implExpr = null;
    if (!(implExprValue instanceof Expr implExprAst)) {
      throw new AssertionError(
          "Definition set function '" + nameStr + "' impl is not an Expr: " + implExprValue);
    }
    var argT = argType;
    if (argT == null) {
      return EResult.err(
          rootLoc, "Definition set function '" + nameStr + "' is missing an arg type");
    }
    var retT = retType;
    if (retT == null) {
      return EResult.err(
          rootLoc, "Definition set function '" + nameStr + "' is missing a return type");
    }
    var scopeId = bindingId;
    if (scopeId == null) {
      throw new AssertionError(
          "Definition set function '" + nameStr + "' is not registered");
    }

    var innerVx = new EvalContext(xx.vx.ix);
    // Source-function bodies reference their own Functions class's static fields (not the
    // outer EvalRoot), because Functions is loaded in a separate class loader that can't reach
    // EvalRoot. DefSetFunctionsEmitter registers builtins/defs on Functions with un-prefixed
    // names matching the body's field references.
    if (scopeId instanceof BindingId.Def defForOwner) {
      innerVx.evalRootInternalName =
          compilerxyz.eval.dynamic.DefSetFunctionsEmitter.functionsInternalName(defForOwner.defSet());
    } else {
      innerVx.evalRootInternalName = xx.vx.evalRootInternalName;
    }
    var innerTarget = new TargetDynamic(innerVx.nextLocalId);
    innerTarget.registerStaticFunctionDefs(xx.vx.ix.staticFunctionDefs);

    var bodyScope = new Scope(null);
    bodyScope.values.putAll(Eval.bodyBuiltinScope(xx));

    var argSlots = new TList<DefSetFunctionsEmitter.ArgSlot>();
    var tupleOpt = AspectTuple.getMaybe(xx, argT);
    var recordOpt = AspectRecord.getMaybe(xx, argT);
    if (tupleOpt.optIsSome()) {
      var elemTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
      var elemBindings = new TList<Binding>();
      for (var et : elemTypes) {
        var slotId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, rootLoc, et)));
        var ibMaybe = AspectInitialBind.getMaybe(xx, et);
        if (!(ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome)) {
          return EResult.err(rootLoc, "arg tuple element type " + et.typeDesc() + " missing AspectInitialBind");
        }
        elemBindings.add(ibSome.value().aspectInitialBindInitialBind(slotId));
      }
      var argTuple = new ArgumentTuple(elemBindings, elemTypes);
      bodyScope.values.put(new ConstKey.Str("arg"), new ArgumentTupleBinding(argTuple));
    } else if (recordOpt.optIsSome()) {
      var fieldTypes = recordOpt.optUnwrap().aspectRecordRecordFieldTypes();
      var fields = new compilerxyz.util.TOrderedMap<ConstKey, ArgumentEntry>();
      for (var entry : fieldTypes.entrySet()) {
        var et = entry.getValue();
        var slotId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, rootLoc, et)));
        var ibMaybe = AspectInitialBind.getMaybe(xx, et);
        if (!(ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome)) {
          return EResult.err(rootLoc, "arg record field type " + et.typeDesc() + " missing AspectInitialBind");
        }
        fields.put(entry.getKey(), new ArgumentEntry(ibSome.value().aspectInitialBindInitialBind(slotId), et));
      }
      var argRec = new ArgumentRecord(fields);
      bodyScope.values.put(new ConstKey.Str("arg"), new ArgumentRecordBinding(argRec));
    } else {
      var slotId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
      argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, rootLoc, argT)));
      var ibMaybe = AspectInitialBind.getMaybe(xx, argT);
      if (ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome) {
        bodyScope.values.put(new ConstKey.Str("arg"), ibSome.value().aspectInitialBindInitialBind(slotId));
      }
    }
    var funcXx =
        new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, exceptionType);

    var implResult = ConstEval.constEval(funcXx, implExprAst);
    if (implResult.hasErrors()) {
      return implResult.reerr();
    }
    var implCv = implResult.requireValue("SourceFunctionData::compile constEval");
    var convertedResult = Eval.typecheckConvert(funcXx, rootLoc, retT, implCv);
    if (convertedResult.hasErrors()) {
      return convertedResult.reerr();
    }
    var convertedCv =
        convertedResult.requireValue("SourceFunctionData::compile typecheckConvert");
    CodeDynamic bodyCode;
    if (convertedCv.value() instanceof Opt.Some<Value> someVal) {
      var valueType = someVal.value().valueTypeOf();
      var consumeResult = AspectConsumeDynamic.get(funcXx, rootLoc, valueType);
      if (consumeResult.hasErrors()) {
        return consumeResult.reerr();
      }
      var consume = consumeResult.requireValue("SourceFunctionData::compile consume aspect");
      var dynResult = consume.toDynamic(funcXx, rootLoc, convertedCv);
      if (dynResult.hasErrors()) {
        return dynResult.reerr();
      }
      bodyCode = dynResult.requireValue("SourceFunctionData::compile consume.toDynamic");
    } else {
      bodyCode = convertedCv.code().dynamic();
      if (bodyCode == null) {
        return EResult.err(
            rootLoc,
            "Definition set function '" + nameStr + "' body produced no compile-time value");
      }
    }

    for (var entry : innerVx.definitionSets.entrySet()) {
      xx.vx.definitionSets.putIfAbsent(entry.getKey(), entry.getValue());
    }

    var owningSet = set;
    for (var entry : innerVx.definitionSets.entrySet()) {
      if (entry.getKey().equals(owningSet.defSetId)) {
        continue;
      }
      owningSet.dependencies.putIfAbsent(entry.getKey(), entry.getValue());
    }

    var captured = innerVx.externals;
    capturedExternals.addAll(captured);

    var defBinding = scopeId;
    if (!(defBinding instanceof BindingId.Def def)) {
      throw new AssertionError(
          "SourceFunctionData.finish: binding id must be a Def, got " + defBinding.getClass());
    }
    var retDescRes = jvmDescForRes(xx, rootLoc, retT);
    if (retDescRes.hasErrors()) return retDescRes.reerr();
    var retDesc = retDescRes.requireValue("SourceFunctionData::finish retDesc");
    functionSpec =
        new DefSetFunctionsEmitter.FunctionSpec(
            def.index(), argSlots, retDesc, bodyCode, TList.from(captured));
    return EResult.ok(null);
  }

  private static compilerxyz.eval.dynamic.JavaDataDescriptor jvmDescFor(
      ExprContext xx, QExprId loc, Type t) {
    var consumeOpt = AspectConsumeDynamic.getMaybe(xx, t);
    if (consumeOpt instanceof Opt.Some<AspectConsumeDynamic> some) {
      return some.value().consumeDynamicJvmClass();
    }
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  private static EResult<compilerxyz.eval.dynamic.JavaDataDescriptor> jvmDescForRes(
      ExprContext xx, QExprId loc, Type t) {
    var consumeOpt = AspectConsumeDynamic.getMaybe(xx, t);
    if (consumeOpt instanceof Opt.Some<AspectConsumeDynamic> some) {
      return EResult.ok(some.value().consumeDynamicJvmClass());
    }
    return EResult.ok(
        compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
            compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT));
  }

  public EResult<Void> instantiate(QExprId rootLoc, DefinitionSetClassLoader loader) {

    var binaryName = compiledClassName;
    if (binaryName == null) {
      throw new AssertionError(
          "SourceFunctionData.instantiate: compiledClassName is null (finish was not called)");
    }
    return EResult.ok(null);
  }
}
