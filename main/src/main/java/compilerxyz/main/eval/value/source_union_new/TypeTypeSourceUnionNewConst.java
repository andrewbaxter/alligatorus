package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactDataSourceUnionNewCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6143L)
public record TypeTypeSourceUnionNewConst(TypeSourceUnionDynamic type, int variantIndex) implements Aspect, Type {

    TypeSourceUnionNewDynamic asCtor() {
        return new TypeSourceUnionNewDynamic(type, variantIndex);
    }

    @Override
    public String typeDesc() {
        return "definition_set_union_new_const";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_NEW_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionTypeSourceUnionNewConst(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindTypeSourceUnionNewConst(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactTypeSourceUnionNewConst.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    private static Artifact nullArtifact() {
        return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (self instanceof TypeSourceUnionNewDynamic dyn) {
                self = new TypeTypeSourceUnionNewConst(dyn.type(), dyn.variantIndex());
            }
            if (!(self instanceof TypeTypeSourceUnionNewConst uc)) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceUnionNewConst::exporterRootDelegateSelfExport: expected"
                                + " TypeTypeSourceUnionNewConst, got " + self.getClass());
            }
            var tu = uc.type;
            var variantIndex = uc.variantIndex;
            if (variantIndex < 0 || variantIndex >= tu.variants.size()) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceUnionNewConst::exporterExport: variant index " + variantIndex + " out of range");
            }
            var variant = tu.variants.get(variantIndex);
            var errors = new TList<ExprErr>();
            var artifact = ArtifactSupport.implExportIndependentArtifact(
                    xx.vx.ix.gx.artifacts, variant.newArtifactIdCell, () -> {
                        var tuArtRes = Artifactable.exportArtifact(xx, loc, tu);
                        tuArtRes.drainInto(errors);
                        var tuArt = tuArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var body = new ArtifactDataSourceUnionNew(tuArt, variantIndex);
                        return makeBody.apply(ArtifactDataSourceUnionNewCodec.toValue(body));
                    });
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeTypeSourceUnionNewConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataSourceUnionNew body;
                        try {
                            body = ArtifactDataSourceUnionNewCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("TypeTypeSourceUnionNewConst data: failed to parse "
                                    + "ArtifactDataSourceUnionNew: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var tuArt = body.union();
                        var variantIndex = body.variantIndex();
                        var deps = new TList<Artifact>();
                        deps.add(tuArt);
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var parentAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, tuArt);
                            var parentVal =
                                    parentAny.requireValue("TypeTypeSourceUnionNewConst::finalizer parent union");
                            var tuData = MustCast.mustCast(TypeSourceUnionDynamic.class, parentVal);
                            if (variantIndex >= 0 && variantIndex < tuData.variants.size()) {
                                tuData.variants
                                        .get(variantIndex)
                                        .newArtifactIdCell
                                        .set(idValue);
                            }
                            return new TypeTypeSourceUnionNewConst(tuData, variantIndex);
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
                GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_NEW_CONST_IDENTITY,
                TypeTypeSourceUnionNewConst.class);
        return oOpt.optIsSome()
                && type.typeIsEqual(xx, oOpt.optUnwrap().type)
                && variantIndex == oOpt.optUnwrap().variantIndex;
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
        return EResult.ok(AspectArtifactTypeSourceUnionNewConst.INSTANCE.aspectArtifactExporter());
    }
}
