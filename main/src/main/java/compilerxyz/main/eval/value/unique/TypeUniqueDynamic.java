package compilerxyz.main.eval.value.unique;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectDropDynamicLightDefault;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6003L)
public record TypeUniqueDynamic() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "unique";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(AspectInitialBindDynamicUniqueDynamic.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftUniqueDynamic.INSTANCE);
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicUniqueDynamic(this));
        }
        if (key.equals(GenAspectOperatorEq.AspectId)) {
            return Opt.some(AspectOperatorEqUniqueDynamic.INSTANCE);
        }
        if (key.equals(GenAspectDropDynamicLight.AspectId)) {
            return Opt.some(AspectDropDynamicLightDefault.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicUniqueDynamic.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactUniqueDynamic.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY) || key.equals(GenAspectLift.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var u = MustCast.mustCast(
                    Unique.class,
                    MustCast.mustCast(ValueObjectConst.class, self).data());
            JsonValue data = encodeUnique(u);
            return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueObject obj -> {
                    var decoded = decodeUnique(loc, obj);
                    if (decoded.hasErrors()) {
                        yield EResult.err(
                                loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: malformed");
                    }
                    yield EResult.ok(new ValueObjectConst(
                            new TypeUniqueDynamic(),
                            decoded.requireValue(
                                    "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData decoded")));
                }
                case JsonValueNull ignored ->
                    EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueBoolean b ->
                    EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueNumber n ->
                    EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueString s ->
                    EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueArray a ->
                    EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            };
        }
    };

    private static JsonValue encodeUnique(Unique u) {
        var m = new TOrderedMap<String, JsonValue>();
        switch (u) {
            case Unique.Builtin b -> {
                var inner = new TOrderedMap<String, JsonValue>();
                inner.put("id", new JsonValueNumber(b.id()));
                m.put("builtin", new JsonValueObject(inner));
            }
            case Unique.Source s -> {
                var inner = new TOrderedMap<String, JsonValue>();
                var ci = new TOrderedMap<String, JsonValue>();
                ci.put(
                        "import_state",
                        new JsonValueNumber(s.compileId().importState().value()));
                ci.put("source", new JsonValueNumber(s.compileId().source().value()));
                inner.put("compile_id", new JsonValueObject(ci));
                inner.put("id", new JsonValueNumber(s.id()));
                m.put("source", new JsonValueObject(inner));
            }
        }
        return new JsonValueObject(m);
    }

    private static EResult<Unique> decodeUnique(QExprId loc, JsonValueObject obj) {
        if (obj.entries().size() != 1) return EResult.err(loc, "expected tagged object");
        var entry = obj.entries().entrySet().iterator().next();
        var tag = entry.getKey();
        JsonValueObject inner;
        switch (entry.getValue()) {
            case JsonValueObject o -> inner = o;
            case JsonValueNull jn -> {
                return EResult.err(loc, "expected inner object");
            }
            case JsonValueBoolean b -> {
                return EResult.err(loc, "expected inner object");
            }
            case JsonValueNumber n -> {
                return EResult.err(loc, "expected inner object");
            }
            case JsonValueString s -> {
                return EResult.err(loc, "expected inner object");
            }
            case JsonValueArray a -> {
                return EResult.err(loc, "expected inner object");
            }
        }
        return switch (tag) {
            case "builtin" -> {
                var idRaw = inner.entries().get("id");
                if (idRaw == null) yield EResult.err(loc, "builtin: missing id");
                else
                    yield switch (idRaw) {
                        case JsonValueNumber n -> EResult.ok(new Unique.Builtin((long) n.value()));
                        case JsonValueNull jn -> EResult.err(loc, "builtin: missing id");
                        case JsonValueBoolean b -> EResult.err(loc, "builtin: missing id");
                        case JsonValueString s -> EResult.err(loc, "builtin: missing id");
                        case JsonValueArray a -> EResult.err(loc, "builtin: missing id");
                        case JsonValueObject o -> EResult.err(loc, "builtin: missing id");
                    };
            }
            case "source" -> {
                var ciRaw = inner.entries().get("compile_id");
                var idRaw = inner.entries().get("id");
                if (ciRaw == null || idRaw == null) yield EResult.err(loc, "source: malformed compile_id/id");
                JsonValueObject ci;
                switch (ciRaw) {
                    case JsonValueObject o -> ci = o;
                    case JsonValueNull jn -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueBoolean b -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueNumber n -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueString s -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueArray a -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                }
                long nVal;
                switch (idRaw) {
                    case JsonValueNumber n -> nVal = (long) n.value();
                    case JsonValueNull jn -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueBoolean b -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueString s -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueArray a -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueObject o -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                }
                var isRaw = ci.entries().get("import_state");
                var srcRaw = ci.entries().get("source");
                if (isRaw == null || srcRaw == null) yield EResult.err(loc, "source: malformed compile_id/id");
                long isVal;
                switch (isRaw) {
                    case JsonValueNumber n -> isVal = (long) n.value();
                    case JsonValueNull jn -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueBoolean b -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueString s -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueArray a -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueObject o -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                }
                long srcVal;
                switch (srcRaw) {
                    case JsonValueNumber n -> srcVal = (long) n.value();
                    case JsonValueNull jn -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueBoolean b -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueString s -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueArray a -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                    case JsonValueObject o -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
                }
                yield EResult.ok(
                        new Unique.Source(new CompileId(new ImportStateId(isVal), new SourceId(srcVal)), nVal));
            }
            default -> EResult.err(loc, "unknown unique tag: " + tag);
        };
    }

    static Opt<Unique> extractUnique(CodeValue cv) {
        if (cv.value().optIsSome()
                && cv.value().optUnwrap() instanceof ValueObjectConst v
                && v.data() instanceof Unique u) {
            return Opt.some(u);
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType
                .typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        var codeCast = CodeDynamic.castOpt(loc, cv.code());
        if (codeCast.hasErrors()) return codeCast.reerr();
        if (codeCast.requireValue("code").optIsSome()) return EResult.ok(cv);
        if (cv.value().optIsNone()) return EResult.ok(cv);
        var srcType = cv.value().optUnwrap().valueType();
        if (srcType instanceof TypeUniqueDynamic) return EResult.ok(cv);
        var asp = GenAspectConsumeDynamic.getMaybe(xx, srcType);
        if (asp.optIsNone()) return EResult.ok(cv);
        var dynRes = asp.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return EResult.ok(
                new CodeValue(Code.target(dynRes.requireValue("TypeUniqueDynamic::typeCastFrom")), cv.outcome()));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return EResult.ok(cv);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.UNIQUE_TYPE);
    }
}
