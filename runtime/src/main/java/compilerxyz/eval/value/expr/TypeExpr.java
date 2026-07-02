package compilerxyz.eval.value.expr;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.Expr;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.empty.ValueEmptyConst;
import compilerxyz.gen.GenIdentities;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import java.util.function.Function;
import compilerxyz.eval.dynamic.DynUtil;

public record TypeExpr()
    implements
        Type,
        Value,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectInitialBind,
        AspectLift,
        AspectType,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "expr";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.Expr();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.EXPR_TYPE_IDENTITY)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectLift.ID)
        || key.equals(AspectType.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.EXPR;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (self instanceof TypeExpr) {
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
          }
          return EResult.err(
              loc,
              "TypeExpr::exporterRootDelegateSelfExport: Expr-value artifact path needs the deferred expr.rs port"
                  + " (got "
                  + self.getClass()
                  + ")");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (data instanceof JsonValueNull) {
            return EResult.ok(new TypeExpr());
          }
          return EResult.err(loc, "TypeExpr::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
        }
      };

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return true;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> someVal)) {
      return EResult.ok(cv);
    }
    var sourceValue = someVal.value();
    var sourceType = sourceValue.valueTypeOf();
    if (sourceType.typeAspect(xx, GenIdentities.EXPR_TYPE_IDENTITY).optIsSome()) {
      if (sourceValue instanceof Expr expr) {
        return EResult.ok(new CodeValue(cv.code(), Opt.some(expr)));
      }
      return EResult.ok(cv);
    }
    var emptyExpr =
        new Expr(
            loc,
            new Expr.Literal(Expr.LitValue.shared(new ValueEmptyConst(sourceType))));
    return EResult.ok(new CodeValue(cv.code(), Opt.some(emptyExpr)));
  }

  @Override
  public EResult<CodeValue> lift(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.err(
        loc, "TypeExpr::lift(CodeValue): staging-side lift wrapper not yet ported");
  }

  @Override
  public EResult<Value> liftRuntime(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(externalHostValue instanceof StagedExprCell cell)) {
      return EResult.err(
          loc,
          "TypeExpr::liftRuntime: expected StagedExprCell, got "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
    var inner = cell.take();
    if (!(inner instanceof Value value)) {
      return EResult.err(
          loc,
          "TypeExpr::liftRuntime: StagedExprCell contained non-Value: "
              + inner.getClass().getName());
    }
    return EResult.ok(value);
  }

  @Override
  public Type type_(Value value) {
    return new TypeExpr();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeExpr::toDynamic: CodeValue has no value to stage");
    }
    var cell = new StagedExprCell(some.value());
    var id = new BindingId.Local(xx.vx.nextLocalId.next());
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(id, new External.StagedExprCellExternal(cell)));
    return EResult.ok(new CodeDynamic(loc, new JavaBytecode.BindingUse(id)));
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    throw new UnsupportedOperationException(
        "TypeExpr::initialBind: needs BindingDynamicBinding port (deferred)");
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
