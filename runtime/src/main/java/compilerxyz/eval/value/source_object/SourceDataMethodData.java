package compilerxyz.eval.value.source_object;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.eval.ContextualFunction;
import org.jspecify.annotations.Nullable;

public final class SourceDataMethodData {
  public final String name;
  public final String fieldName;
  public @Nullable Type argType;
  public @Nullable Type retType;
  public @Nullable Value implExpr;
  public @Nullable String compiledClassName;
  public @Nullable byte[] compiledBytecode;
  public @Nullable BindingId bindingId;
  public @Nullable BindingId innerBindingId;
  public final TList<EvalContext.ExternalEntry> capturedExternals;
  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;
  public @Nullable ContextualFunction compiledFunctionInstance;

  public @Nullable CodeDynamic compiledBodyCode;

  public BindingId.@Nullable Local compiledArgBindingId;

  public SourceDataMethodData(String name, String fieldName) {
    this.name = name;
    this.fieldName = fieldName;
    this.capturedExternals = new TList<>();
  }

  public EResult<Void> finish(
      ExprContext xx,
      QExprId rootLoc,
      String dtName,
      Type underlyingType,
      String dataField) {
    if (compiledBodyCode != null) {
      return EResult.ok(null);
    }
    var implExprValue = implExpr;
    if (implExprValue == null) {

      return EResult.ok(null);
    }
    implExpr = null;
    if (!(implExprValue instanceof Expr implExprAst)) {
      throw new AssertionError(
          "Data type '" + dtName + "' method '" + name + "' impl is not an Expr: " + implExprValue);
    }
    var retT = retType;
    if (retT == null) {
      return EResult.err(
          rootLoc, "Data type '" + dtName + "' method '" + name + "' is missing a return type");
    }
    if (bindingId == null) {
      throw new AssertionError(
          "Data type '" + dtName + "' method '" + name + "' is not registered");
    }

    var innerVx = new EvalContext(xx.vx.ix);
    var innerTarget = new TargetDynamic(innerVx.nextLocalId);
    innerTarget.registerStaticFunctionDefs(xx.vx.ix.staticFunctionDefs);
    var selfId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    innerBindingId = selfId;
    var argId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());

    var bodyScope = new Scope(null);
    bodyScope.values.putAll(Eval.bodyBuiltinScope(xx));
    var selfIb = AspectInitialBind.getMaybe(xx, underlyingType);
    if (selfIb instanceof Opt.Some<AspectInitialBind> selfIbSome) {
      bodyScope.values.put(new ConstKey.Str("inner"), selfIbSome.value().initialBind(selfId));
    }
    if (argType != null) {
      var argIbMaybe = AspectInitialBind.getMaybe(xx, argType);
      if (argIbMaybe instanceof Opt.Some<AspectInitialBind> argIbSome) {
        bodyScope.values.put(new ConstKey.Str("arg"), argIbSome.value().initialBind(argId));
      }
    }
    var methodXx =
        new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, null);

    var implResult = ConstEval.constEval(methodXx, implExprAst);
    if (implResult.hasErrors()) {
      return implResult.reerr();
    }
    var implCv = implResult.valueOrNull();
    if (implCv == null) {
      throw new IllegalStateException("constEval returned neither value nor errors");
    }
    var convertedResult = Eval.typecheckConvert(methodXx, rootLoc, retT, implCv);
    if (convertedResult.hasErrors()) {
      return convertedResult.reerr();
    }
    var convertedCv = convertedResult.valueOrNull();
    if (convertedCv == null) {
      throw new IllegalStateException("typecheckConvert returned neither value nor errors");
    }
    CodeDynamic bodyCode;
    if (convertedCv.value() instanceof Opt.Some<Value> someVal) {
      var valueType = someVal.value().valueTypeOf();
      var consumeResult = AspectConsumeDynamic.get(methodXx, rootLoc, valueType);
      if (consumeResult.hasErrors()) {
        return consumeResult.reerr();
      }
      var consume = consumeResult.valueOrNull();
      if (consume == null) {
        throw new IllegalStateException("AspectConsumeDynamic.get returned neither value nor errors");
      }
      var dynResult = consume.toDynamic(methodXx, rootLoc, convertedCv);
      if (dynResult.hasErrors()) {
        return dynResult.reerr();
      }
      bodyCode = dynResult.valueOrNull();
      if (bodyCode == null) {
        throw new IllegalStateException("AspectConsumeDynamic.toDynamic returned neither value nor errors");
      }
    } else {
      bodyCode = convertedCv.code().dynamic();
      if (bodyCode == null) {
        return EResult.err(
            rootLoc,
            "Data type '" + dtName + "' method '" + name + "' body produced no compile-time value");
      }
    }

    for (var entry : innerVx.definitionSets.entrySet()) {
      xx.vx.definitionSets.putIfAbsent(entry.getKey(), entry.getValue());
    }

    var captured = innerVx.externals;
    capturedExternals.addAll(captured);

    var methodArgId = argType != null ? argId : null;

    compiledBodyCode = bodyCode;
    compiledArgBindingId = methodArgId;
    return EResult.ok(null);
  }

  public EResult<Void> instantiate(QExprId rootLoc, DefinitionSetClassLoader loader) {
    if (compiledFunctionInstance != null) {
      return EResult.ok(null);
    }
    var binaryName = compiledClassName;
    if (binaryName == null) {

      return EResult.ok(null);
    }
    Class<?> cls;
    try {
      cls = Class.forName(binaryName, true, loader);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(
          "SourceDataMethodData.instantiate: generated class " + binaryName + " failed to load", e);
    }
    Object instance;
    try {
      instance = cls.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "SourceDataMethodData.instantiate: generated class "
              + binaryName
              + " failed to instantiate",
          e);
    }
    @SuppressWarnings("unchecked")
    var fn = (ContextualFunction) instance;
    compiledFunctionInstance = fn;
    return EResult.ok(null);
  }
}
