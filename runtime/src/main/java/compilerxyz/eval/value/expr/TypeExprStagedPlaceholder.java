package compilerxyz.eval.value.expr;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectLift;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.util.Opt;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeExprStagedPlaceholder()
    implements Type, Value, AspectConsumeDynamic, AspectDropDynamicLight, AspectLift, ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_EXPR_STAGED_PLACEHOLDER_TYPE_ID; }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "stage result";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.ExprStagedPlaceholder();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID) || key.equals(AspectLift.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(externalHostValue instanceof StagedExprCell cell)) {
      return EResult.err(
          loc,
          "TypeExprStagedPlaceholder::lift: expected StagedExprCell, got "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
    var inner = cell.take();
    if (!(inner instanceof Value value)) {
      return EResult.err(
          loc,
          "TypeExprStagedPlaceholder::lift: StagedExprCell contained non-Value: "
              + inner.getClass().getName());
    }
    return EResult.ok(value);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeExprStagedPlaceholder::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
