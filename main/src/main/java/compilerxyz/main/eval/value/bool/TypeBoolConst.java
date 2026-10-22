package compilerxyz.main.eval.value.bool;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindConstValue;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.AspectConstDynamicOf;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBoolConst;
import compilerxyz.main.gen.GenAspectConstDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectOperatorBitNot;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6102L)
public record TypeBoolConst() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "bool";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectConstDynamic.AspectId)) {
            return Opt.some(new AspectConstDynamicOf(new TypeBoolDynamic()));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_BOOL_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectOperatorBitNot.AspectId)) {
            return Opt.some(AspectOperatorBitNotBoolConst.INSTANCE);
        }
        if (key.equals(GenAspectOperatorEq.AspectId)) {
            return Opt.some(AspectOperatorEqBoolConst.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicBoolConst.INSTANCE);
        }
        if (key.equals(GenAspectBoolConst.AspectId)) {
            return Opt.some(AspectBoolConstBoolConst.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindConstValue.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactBoolConst.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftBoolConst.INSTANCE);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var bv = MustCast.mustCast(Boolean.class, self);
            JsonValue data = new JsonValueBoolean(bv);
            return EResult.ok(new Artifact.Inline(makeBody.apply(data)));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueBoolean b -> EResult.ok(b.value());
                default ->
                    EResult.err(loc, "TypeBoolConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            };
        }
    };

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_BOOL_CONST_IDENTITY)
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
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.BOOL_CONST_TYPE);
    }
}
