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
import compilerxyz.gen.AspectLift;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.util.Opt;

public record TypeExprStagedPlaceholder()
    implements Type, Value, AspectConsumeDynamic, AspectLift, ExporterAny {

  @Override
  public String typeDesc() {
    return "stage result";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.ExprStagedPlaceholder();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID) || key.equals(AspectLift.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override  @Override
  public EResult<Value> lift(
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
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
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
}
