package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataSourceObjectTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6138L)
public record TypeTypeSourceObjectConst(TypeSourceObjectDynamic type_) implements Type, AspectArtifact, AspectType {

    @Override
    public String typeDesc() {
        return type_.description.optUnwrapOr("object(" + type_.name.optUnwrapOr("?") + ")");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_CONST_IDENTITY)
                || key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectType.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.OBJECT_CONST;
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.SOURCE_OBJECT_TYPE_CONST);
    }

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var oc = MustCast.mustCast(TypeTypeSourceObjectConst.class, self);
            var objRes = Artifactable.exportArtifact(xx, loc, oc.type_);
            if (objRes.hasErrors()) return objRes.reerr();
            var body =
                    new ArtifactDataSourceObjectType(objRes.requireValue("TypeTypeSourceObjectConst::export object"));
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactDataSourceObjectTypeCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactDataSourceObjectType body;
            try {
                body = ArtifactDataSourceObjectTypeCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceObjectConst data: failed to parse ArtifactDataSourceObjectType: "
                                + e.getMessage());
            }
            var objRes = ArtifactSupport.importArtifact(xx, loc, body.object());
            if (objRes.hasErrors()) return objRes.reerr();
            return EResult.ok(new TypeTypeSourceObjectConst(MustCast.mustCast(
                    TypeSourceObjectDynamic.class, objRes.requireValue("TypeTypeSourceObjectConst::import object"))));
        }
    };

    @Override
    public Type aspectTypeType(Value value) {
        var voc = MustCast.mustCast(ValueObjectConst.class, value);
        return MustCast.mustCast(Type.class, voc.data());
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_CONST_IDENTITY,
                TypeTypeSourceObjectConst.class);
        return oOpt.optIsSome() && type_.typeIsEqual(xx, oOpt.optUnwrap().type_);
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
}
