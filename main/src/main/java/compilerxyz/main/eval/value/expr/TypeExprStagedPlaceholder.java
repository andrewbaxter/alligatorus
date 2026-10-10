package compilerxyz.main.eval.value.expr;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectConsumeDynamicObject;
import compilerxyz.main.eval.value.object.TypeObjectDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.GenTypeExprStagedPlaceholder;
import compilerxyz.main.gen.Value;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaSupport;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6107L)
public record TypeExprStagedPlaceholder() implements Aspect, Type, Value {

    @Override
    public String typeDesc() {
        return "stage result";
    }

    @Override
    public Meta typeMeta(ExprContext xx) {
        return MetaSupport.derive(xx, this, "expr_staged_placeholder");
    }

    public Type valueType() {
        return ((TypeObjectDynamic) GenTypeExprStagedPlaceholder.dynamicType()).pairedConst();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_EXPR_STAGED_PLACEHOLDER_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftStagedExprCell.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicObject.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_EXPR_STAGED_PLACEHOLDER_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
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
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
