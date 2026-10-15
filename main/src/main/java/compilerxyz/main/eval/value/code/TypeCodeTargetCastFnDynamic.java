package compilerxyz.main.eval.value.code;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6361L, function = true)
public record TypeCodeTargetCastFnDynamic() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "code.target.cast";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionCodeTargetCastFnDynamic(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_CODE_TARGET_CAST_FN_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CODE_TARGET_CAST_FN_DYNAMIC_IDENTITY)
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
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
}
