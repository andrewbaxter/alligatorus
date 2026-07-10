package compilerxyz.eval.value.expr;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
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
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.Expr;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.gen.GenIdentities;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import java.util.function.Function;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeExpr()
    implements
        Type,
        Value,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectInitialBind,
        AspectLift,
        AspectType,
        ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_EXPR_TYPE_ID; }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JavaInternalName.of("compilerxyz/eval/expr/StagedExprCell"));
  }

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
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectLift.ID)
        || key.equals(AspectType.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
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
    var literalExpr =
        new Expr(loc, new Expr.Literal(Expr.LitValue.shared(sourceValue)));
    return EResult.ok(new CodeValue(cv.code(), Opt.some(literalExpr)));
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(externalHostValue instanceof StagedExprCell cell)) {
      return EResult.err(
          loc,
          "TypeExpr::lift: expected StagedExprCell, got "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
    var inner = cell.take();
    if (!(inner instanceof Value value)) {
      return EResult.err(
          loc,
          "TypeExpr::lift: StagedExprCell contained non-Value: "
              + inner.getClass().getName());
    }
    return EResult.ok(value);
  }

  @Override
  public Type type_(Value value) {
    return new TypeExpr();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeExpr::toDynamic: CodeValue has no value to stage");
    }
    var cell = new StagedExprCell(some.value());
    var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var id = new BindingId.Local(key);
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(key, this, new External(StagedExprCell.class, cell)));
    return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
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

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
