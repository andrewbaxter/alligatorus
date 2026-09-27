package compilerxyz.main.eval.value.source_object_new;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
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
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactDataSourceObjectNewCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6140L)
public record TypeTypeSourceObjectNewConst(TypeSourceObjectDynamic type) implements Aspect, Type {

    TypeSourceObjectNewDynamic asCtor() {
        return new TypeSourceObjectNewDynamic(type);
    }

    @Override
    public String typeDesc() {
        return type.description.optUnwrapOr("definition_set_object_new_const");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_NEW_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(new AspectLiftTypeSourceObjectNewConst(this));
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionTypeSourceObjectNewConst(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindTypeSourceObjectNewConst(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactTypeSourceObjectNewConst.INSTANCE);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeTypeSourceObjectNewConst oc)) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceObjectNewConst::exporterRootDelegateSelfExport: expected"
                                + " TypeTypeSourceObjectNewConst, got " + self.getClass());
            }
            var dt = oc.type;
            return ArtifactSupport.implExportIndependentArtifact(xx, dt.newArtifactIdCell, () -> {
                var objectArtRes = Artifactable.exportArtifact(xx, loc, dt);
                if (objectArtRes.hasErrors()) return objectArtRes.reerr();
                var body = new ArtifactDataSourceObjectNew(
                        objectArtRes.requireValue("TypeTypeSourceObjectNewConst::export object"));
                return EResult.ok(makeBody.apply(ArtifactDataSourceObjectNewCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeTypeSourceObjectNewConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataSourceObjectNew body;
                        try {
                            body = ArtifactDataSourceObjectNewCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("TypeTypeSourceObjectNewConst data: failed to parse "
                                    + "ArtifactDataSourceObjectNew: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var objectArt = body.object();
                        var deps = new TList<Artifact>();
                        deps.add(objectArt);
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var parentAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, objectArt);
                            var parentVal =
                                    parentAny.requireValue("TypeTypeSourceObjectNewConst::finalizer parent object");
                            var dtData = MustCast.mustCast(TypeSourceObjectDynamic.class, parentVal);
                            dtData.newArtifactIdCell.set(idValue);
                            return new TypeTypeSourceObjectNewConst(dtData);
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

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
                GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_NEW_CONST_IDENTITY,
                TypeTypeSourceObjectNewConst.class);
        return oOpt.optIsSome() && type.typeIsEqual(xx, oOpt.optUnwrap().type);
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
        return EResult.ok(AspectArtifactTypeSourceObjectNewConst.INSTANCE.aspectArtifactExporter());
    }
}
