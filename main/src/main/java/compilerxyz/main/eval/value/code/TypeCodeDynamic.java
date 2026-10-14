package compilerxyz.main.eval.value.code;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinPath("types.code")
@BuiltinFull(uniqueId = 6300L)
public record TypeCodeDynamic() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "code";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindCodeDynamic(this));
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(AspectUnionCodeDynamic.INSTANCE);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicCodeDynamic(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftCodeDynamic.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicCodeDynamic(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_CODE_DYNAMIC_IDENTITY)
                || key.equals(GenTrivialIdentityAspects.CODE_IDENTITY)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.CODE_DYNAMIC);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CODE_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType
                .typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
}
