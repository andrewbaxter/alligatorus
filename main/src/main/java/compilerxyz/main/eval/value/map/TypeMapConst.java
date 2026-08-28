package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter.Root;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactMapTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6162L)
public final class TypeMapConst<K extends Type, V extends Type> implements Type, AspectArtifact, ExporterAny {

    public final K keyType;
    public final V valueType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeMapConst(K keyType, V valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public String typeDesc() {
        return "const map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_CONST_IDENTITY) || key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.MAP_CONST;
    }

    private static Artifact nullArtifact() {
        return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new Root()));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, ExporterAny self, Function<JsonValue, ArtifactBody> makeBody) {
            TypeMapConst map;
            if (self instanceof TypeMapConst<?, ?> t) {
                map = t;
            } else if (self instanceof ValueObjectConst v && v.type() instanceof TypeMapConst<?, ?> t) {
                map = t;
            } else {
                return EResult.err(
                        loc,
                        "TypeMapConst::exporterRootDelegateSelfExport: expected TypeMapConst, got " + self.getClass());
            }
            var cell = map.artifactIdCell;
            var errors = new TList<ExprErr>();
            var artifact = ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, cell, () -> {
                var keyArtRes = map.keyType.artifactableExportArtifact(xx, loc);
                keyArtRes.drainInto(errors);
                var keyArt = keyArtRes.valueOrNull() != null
                        ? keyArtRes.requireValue("TypeMapConst::exporterExport key")
                        : nullArtifact();
                var valueArtRes = map.valueType.artifactableExportArtifact(xx, loc);
                valueArtRes.drainInto(errors);
                var valueArt = valueArtRes.valueOrNull() != null
                        ? valueArtRes.requireValue("TypeMapConst::exporterExport value")
                        : nullArtifact();
                var body = new ArtifactMapType(keyArt, valueArt);
                return makeBody.apply(ArtifactMapTypeCodec.toValue(body));
            });
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeMapConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactMapType body;
                        try {
                            body = ArtifactMapTypeCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "TypeMapConst data: failed to parse ArtifactMapType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var holder = new ExporterAny[] {null};
                        ExporterAny stub = new ExporterAny() {
                            @Override
                            public EResult<Type> exporterAnyAsType() {
                                if (holder[0] == null) {
                                    throw new IllegalStateException("TypeMapConst import stub accessed pre-finalize");
                                }
                                return holder[0].exporterAnyAsType();
                            }

                            @Override
                            public EResult<Value> exporterAnyAsValue() {
                                if (holder[0] == null) {
                                    throw new IllegalStateException("TypeMapConst import stub accessed pre-finalize");
                                }
                                return holder[0].exporterAnyAsValue();
                            }
                        };
                        var deps = new TList<Artifact>();
                        deps.add(body.keyType());
                        deps.add(body.valueType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var keyRes =
                                    ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, body.keyType());
                            var keyT = keyRes.requireValue("TypeMapConst::finalizer key type");
                            var valueRes =
                                    ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, body.valueType());
                            var valueT = valueRes.requireValue("TypeMapConst::finalizer value type");
                            var out = new TypeMapDynamic(keyT, valueT);
                            out.artifactIdCell.set(idValue);
                            holder[0] = out;
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
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var someOpt = other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_MAP_CONST_IDENTITY);
        return someOpt.optIsSome()
                && someOpt.optUnwrap() instanceof TypeMapConst<?, ?> o
                && keyType.typeIsEqual(xx, o.keyType)
                && valueType.typeIsEqual(xx, o.valueType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
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
