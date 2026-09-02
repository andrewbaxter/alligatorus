package compilerxyz.main.eval.value.unique;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.bool_.TypeBoolConst;
import compilerxyz.main.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
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
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6003L)
public record TypeUniqueDynamic()
        implements Type,
                AspectArtifact,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectOperatorEq,
                AspectStackValueDynamic,
                AspectLift,
                AspectBindDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(compilerxyz.main.eval.Unique.class);
    }

    @Override
    public String typeDesc() {
        return "unique";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY)
                || key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectOperatorEq.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.UNIQUE;
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

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dynamicConsumeToDynamic(loc, cv);
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, new TypeUniqueDynamic());
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var u = MustCast.mustCast(Unique.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeUniqueConst(), u));
    }

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        var b = extractUnique(base);
        var r = extractUnique(reference);
        if (b.optIsSome() && r.optIsSome()) {
            return EResult.ok(new CodeValue(
                    Code.none(loc),
                    EvalOutcome.ofValue(new ValueObjectConst(
                            new TypeBoolConst(), b.optUnwrap().equals(r.optUnwrap())))));
        }
        if (base.value().optIsNone() || reference.value().optIsNone()) {
            return EResult.err(loc, "TypeUniqueDynamic::eq: operand has no value");
        }
        var baseAspRes =
                GenAspectConsumeDynamic.get(xx, loc, base.value().optUnwrap().valueType());
        if (baseAspRes.hasErrors()) return baseAspRes.reerr();
        var baseDyn = baseAspRes
                .requireValue("TypeUniqueDynamic::eq base aspect")
                .aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: base lowering failed");
        var refAspRes = GenAspectConsumeDynamic.get(
                xx, loc, reference.value().optUnwrap().valueType());
        if (refAspRes.hasErrors()) return refAspRes.reerr();
        var refDyn = refAspRes
                .requireValue("TypeUniqueDynamic::eq ref aspect")
                .aspectConsumeDynamicToDynamic(xx, loc, reference);
        if (refDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: reference lowering failed");
        var _eueBase = baseDyn.requireValue("TypeUniqueDynamic::eq base dynamic");
        var _eueRef = refDyn.requireValue("TypeUniqueDynamic::eq reference dynamic");
        var _eueSeq = new JavaBytecodeSequence();
        _eueSeq.add(_eueBase);
        _eueSeq.add(_eueRef);
        _eueSeq.add(JavaBytecodeUtils.callMethod(
                loc,
                JavaInternalName.fromClass(Object.class),
                "equals",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_BOOL, TList.of(JavaDataDescriptor.fromClass(Object.class)))));
        var call = new CodeDynamic(loc, _eueSeq);
        return EResult.ok(
                new CodeValue(Code.of(call), EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeBoolDynamic()))));
    }

    private static Opt<Unique> extractUnique(CodeValue cv) {
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
        if (cv.code().codeDynamic() != null) return EResult.ok(cv);
        if (cv.value().optIsNone()) return EResult.ok(cv);
        var srcType = cv.value().optUnwrap().valueType();
        if (srcType instanceof TypeUniqueDynamic) return EResult.ok(cv);
        var asp = GenAspectConsumeDynamic.getMaybe(xx, srcType);
        if (asp.optIsNone()) return EResult.ok(cv);
        var dynRes = asp.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return EResult.ok(new CodeValue(Code.of(dynRes.requireValue("TypeUniqueDynamic::typeCastFrom")), cv.outcome()));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return EResult.ok(cv);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.UNIQUE_TYPE);
    }
}
