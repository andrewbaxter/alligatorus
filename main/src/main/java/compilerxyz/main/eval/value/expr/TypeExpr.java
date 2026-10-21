package compilerxyz.main.eval.value.expr;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.ArtifactExpr;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprArtifacts;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.value.object.TypeObjectDynamic;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.GenTypeExpr;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactExprCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6013L)
@BuiltinPath("types.expr")
public record TypeExpr() implements Aspect, Type, Value {

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var expr = MustCast.mustCast(Expr.class, self);
            var artRes = ExprArtifacts.exprToArtifact(xx, expr);
            if (artRes.hasErrors()) return artRes.reerr();
            return EResult.ok(new Artifact.Body(
                    makeBody.apply(ArtifactExprCodec.toValue(artRes.requireValue("TypeExpr::export artifact")))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactExpr body;
            try {
                body = ArtifactExprCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "TypeExpr data: failed to parse ArtifactExpr: " + e.getMessage());
            }
            var exprRes = ExprArtifacts.exprFromArtifact(xx, body);
            if (exprRes.hasErrors()) return exprRes.reerr();
            return EResult.ok(exprRes.requireValue("TypeExpr::import expr"));
        }
    };

    @Override
    public String typeDesc() {
        return "expr";
    }

    public Type valueType() {
        return ((TypeObjectDynamic) GenTypeExpr.dynamicType()).pairedConst();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindExpr(this));
        }
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(AspectTypeExpr.INSTANCE);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftStagedExprCell.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicExpr(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicExpr(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactExpr.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_EXPR_IDENTITY) || key.equals(GenAspectLift.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_EXPR_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return true;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.ok(cv);
        }
        var sourceValue = cv.value().optUnwrap();
        var sourceType = sourceValue.valueType();
        if (sourceType
                .typeAspect(xx, GenTrivialIdentityAspects.TYPE_EXPR_IDENTITY)
                .optIsSome()) {
            if (sourceValue instanceof Expr expr) {
                return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(expr)));
            }
            return EResult.ok(cv);
        }
        var literalExpr = new Expr(loc, new Expr.Literal(new Expr.LitValue(sourceValue)));
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(literalExpr)));
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.EXPR);
    }

    @Override
    public Opt<ArtifactExporter> valueArtifactExporter(ExprContext xx) {
        return typeArtifactExporter(xx);
    }
}
