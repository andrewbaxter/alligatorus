package compilerxyz.main.eval.value.unique;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
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
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueNumberFloat;
import compilerxyz.main.safejson.JsonValueNumberInt;
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
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(AspectInitialBindUniqueDynamic.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftUniqueDynamic.INSTANCE);
        }
        if (key.equals(GenAspectOperatorEq.AspectId)) {
            return Opt.some(AspectOperatorEqUniqueDynamic.INSTANCE);
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

    static JsonValue encodeUnique(Unique u) {
        var m = new TOrderedMap<String, JsonValue>();
        switch (u) {
            case Unique.Builtin b -> {
                var inner = new TOrderedMap<String, JsonValue>();
                inner.put("id", new JsonValueNumberInt(b.id()));
                m.put("builtin", new JsonValueObject(inner));
            }
            case Unique.Source s -> {
                var inner = new TOrderedMap<String, JsonValue>();
                var ci = new TOrderedMap<String, JsonValue>();
                ci.put(
                        "import_state",
                        new JsonValueNumberInt(s.compileId().importState().value()));
                ci.put("source", new JsonValueNumberInt(s.compileId().source().value()));
                inner.put("compile_id", new JsonValueObject(ci));
                inner.put("id", new JsonValueNumberInt(s.id()));
                m.put("source", new JsonValueObject(inner));
            }
        }
        return new JsonValueObject(m);
    }

    static EResult<Unique> decodeUnique(QExprId loc, JsonValueObject obj) {
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
                var idRawOpt = inner.entries().getOpt("id");
                if (idRawOpt.optIsNone()) yield EResult.err(loc, "builtin: missing id");
                var idRaw = idRawOpt.optUnwrap();
                yield switch (idRaw) {
                    case JsonValueNumberInt n -> EResult.ok(new Unique.Builtin(n.value()));
                    case JsonValueNumberFloat n -> EResult.err(loc, "builtin: missing id");
                    case JsonValueNull jn -> EResult.err(loc, "builtin: missing id");
                    case JsonValueBoolean b -> EResult.err(loc, "builtin: missing id");
                    case JsonValueString s -> EResult.err(loc, "builtin: missing id");
                    case JsonValueArray a -> EResult.err(loc, "builtin: missing id");
                    case JsonValueObject o -> EResult.err(loc, "builtin: missing id");
                };
            }
            case "source" -> {
                var ciRawOpt = inner.entries().getOpt("compile_id");
                var idRawOpt = inner.entries().getOpt("id");
                if (ciRawOpt.optIsNone() || idRawOpt.optIsNone())
                    yield EResult.err(loc, "source: malformed compile_id/id");
                var ciRaw = ciRawOpt.optUnwrap();
                var idRaw = idRawOpt.optUnwrap();
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
                    case JsonValueNumberInt n -> nVal = n.value();
                    case JsonValueNumberFloat n -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
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
                var isRawOpt = ci.entries().getOpt("import_state");
                var srcRawOpt = ci.entries().getOpt("source");
                if (isRawOpt.optIsNone() || srcRawOpt.optIsNone())
                    yield EResult.err(loc, "source: malformed compile_id/id");
                var isRaw = isRawOpt.optUnwrap();
                var srcRaw = srcRawOpt.optUnwrap();
                long isVal;
                switch (isRaw) {
                    case JsonValueNumberInt n -> isVal = n.value();
                    case JsonValueNumberFloat n -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
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
                    case JsonValueNumberInt n -> srcVal = n.value();
                    case JsonValueNumberFloat n -> {
                        yield EResult.err(loc, "source: malformed compile_id/id");
                    }
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
        if (DynUtil.dynamicHasCode(cv)) return EResult.ok(cv);
        if (cv.value().optIsNone()) return EResult.ok(cv);
        var srcType = cv.value().optUnwrap().valueType();
        if (srcType instanceof TypeUniqueDynamic) return EResult.ok(cv);
        var asp = GenAspectConsumeDynamic.getMaybe(xx, srcType);
        if (asp.optIsNone()) return EResult.ok(cv);
        var dynRes = asp.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return EResult.ok(new CodeValue(
                dynRes.requireValue("TypeUniqueDynamic::typeCastFrom").code(), cv.outcome()));
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
