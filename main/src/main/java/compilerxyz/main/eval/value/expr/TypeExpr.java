package compilerxyz.main.eval.value.expr;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.value.object.TypeObjectDynamic;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.GenTypeExpr;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6013L)
@BuiltinPath("types.expr")
public record TypeExpr() implements Aspect, Type, Value {

    @Override
    public String typeDesc() {
        return "expr";
    }

    public Type valueType() {
        return ((TypeObjectDynamic) GenTypeExpr.dynamicType()).pairedConst();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicExpr(this));
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
        var literalExpr = new Expr(loc, new Expr.Literal(Expr.LitValue.shared(sourceValue)));
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.EXPR);
    }
}
