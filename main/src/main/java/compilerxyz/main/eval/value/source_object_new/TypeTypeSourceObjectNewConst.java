package compilerxyz.main.eval.value.source_object_new;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.Code;
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
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataSourceObjectNewCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6140L)
public record TypeTypeSourceObjectNewConst(TypeSourceObjectDynamic type_)
        implements Type, AspectArtifact, AspectBind, AspectDropDynamicLight, AspectFunction, AspectLift, ExporterAny {

    private TypeSourceObjectNewDynamic asCtor() {
        return new TypeSourceObjectNewDynamic(type_);
    }

    @Override
    public String typeDesc() {
        return type_.description.optUnwrapOr("definition_set_object_new_const");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_NEW_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectFunction.AspectId)
                || key.equals(GenAspectLift.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return asCtor().aspectBindBind(xx, loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        return asCtor().aspectFunctionCall(xx, loc, function, argument);
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        return EResult.ok(new ValueObjectConst(new TypeSourceObjectNewDynamic(type_), null));
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_OBJECT_NEW_CONST;
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(xx, loc, cv);
    }

    private static Artifact nullArtifact() {
        return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, ExporterAny self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeTypeSourceObjectNewConst oc)) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceObjectNewConst::exporterRootDelegateSelfExport: expected"
                                + " TypeTypeSourceObjectNewConst, got " + self.getClass());
            }
            var dt = oc.type_;
            var errors = new TList<ExprErr>();
            var artifact =
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, dt.newArtifactIdCell, () -> {
                        var parentConst = new TypeTypeSourceObjectConst(dt);
                        var objectArtRes = parentConst.artifactableExportArtifact(xx, loc);
                        objectArtRes.drainInto(errors);
                        var objectArt = objectArtRes.valueOrNull() != null
                                ? objectArtRes.requireValue(
                                        "TypeTypeSourceObjectNewConst::exporterExport parent object")
                                : nullArtifact();
                        var body = new ArtifactDataSourceObjectNew(objectArt);
                        return makeBody.apply(ArtifactDataSourceObjectNewCodec.toValue(body));
                    });
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
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
                        var holder = new ExporterAny[] {null};
                        ExporterAny stub = new ExporterAny() {
                            @Override
                            public EResult<Type> exporterAnyAsType() {
                                if (holder[0] == null) {
                                    throw new IllegalStateException(
                                            "TypeTypeSourceObjectNewConst import stub accessed pre-finalize");
                                }
                                return holder[0].exporterAnyAsType();
                            }

                            @Override
                            public EResult<Value> exporterAnyAsValue() {
                                if (holder[0] == null) {
                                    throw new IllegalStateException(
                                            "TypeTypeSourceObjectNewConst import stub accessed pre-finalize");
                                }
                                return holder[0].exporterAnyAsValue();
                            }
                        };
                        var deps = new TList<Artifact>();
                        deps.add(objectArt);
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var parentAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, objectArt);
                            var parentVal =
                                    parentAny.requireValue("TypeTypeSourceObjectNewConst::finalizer parent object");
                            TypeSourceObjectDynamic dtData;
                            if (parentVal instanceof TypeTypeSourceObjectConst pConst) {
                                dtData = pConst.type_();
                            } else if (parentVal instanceof TypeTypeSourceObjectConst.ImportedSourceObjectConst imp) {
                                dtData = imp.type_();
                            } else {
                                throw new IllegalStateException(
                                        "TypeTypeSourceObjectNewConst finalizer: parent object artifact resolved to "
                                                + parentVal.getClass());
                            }
                            dtData.newArtifactIdCell.set(idValue);
                            holder[0] = new TypeTypeSourceObjectNewConst(dtData);
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(stub, deps, fin));
                    });
        }
    };

    @Override
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
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
                GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_NEW_CONST_IDENTITY,
                TypeTypeSourceObjectNewConst.class);
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

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
