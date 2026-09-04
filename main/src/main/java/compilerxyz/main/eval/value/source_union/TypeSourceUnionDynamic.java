package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Counter;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.MatchDynamic;
import compilerxyz.main.eval.dynamic.SourceUnionEmitter;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.AspectSet;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.codec.ArtifactDataSourceUnionCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6021L)
public final class TypeSourceUnionDynamic
        implements Type,
                AspectInitialBindDynamic,
                AspectFieldUseDynamic,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectSet,
                AspectStackValueDynamic,
                AspectUnion,
                MatchDynamic {

    @SuppressWarnings("NullAway.Init")
    public final DefinitionSet set;

    public final String baseBinaryName;

    public final Unique identity;
    public final Counter nextTagId;
    public final TList<SourceUnionVariant> variants;
    public final TList<Integer> active;
    public final ArtifactIdHolder artifactIdCell;
    public final SourceUnionCompiled compiled;

    public TypeSourceUnionDynamic(DefinitionSet set, Unique identity, String baseBinaryName) {
        this.set = set;
        this.baseBinaryName = baseBinaryName;
        this.identity = identity;
        this.nextTagId = new Counter();
        this.variants = new TList<>();
        this.active = new TList<>();
        this.artifactIdCell = new ArtifactIdHolder();
        this.compiled = new SourceUnionCompiled();
    }

    @SuppressWarnings("NullAway")
    public TypeSourceUnionDynamic(Unique identity, String baseBinaryName) {
        this.set = (DefinitionSet) null;
        this.baseBinaryName = baseBinaryName;
        this.identity = identity;
        this.nextTagId = new Counter();
        this.variants = new TList<>();
        this.active = new TList<>();
        this.artifactIdCell = new ArtifactIdHolder();
        this.compiled = new SourceUnionCompiled();
    }

    private TypeSourceUnionDynamic(TypeSourceUnionDynamic base, TList<Integer> active) {
        this.set = base.set;
        this.baseBinaryName = base.baseBinaryName;
        this.identity = base.identity;
        this.nextTagId = base.nextTagId;
        this.variants = base.variants;
        this.active = active;
        this.artifactIdCell = base.artifactIdCell;
        this.compiled = base.compiled;
    }

    public TypeSourceUnionDynamic withActiveVariants(TList<Integer> newActive) {
        var normalized = new TList<Integer>(newActive.size());
        for (var i : newActive) {
            if (!normalized.contains(i)) normalized.add(i);
        }
        normalized.sort(Integer::compareTo);
        return new TypeSourceUnionDynamic(this, normalized);
    }

    public static Opt<TypeSourceUnionDynamic> unionOf(ExprContext xx, Type t) {
        var dynOpt = AspectLookup.getMaybe(
                xx, t, GenTrivialIdentityAspects.TYPE_SOURCE_UNION_DYNAMIC_IDENTITY, TypeSourceUnionDynamic.class);
        return dynOpt;
    }

    public void initSetReflectively(DefinitionSet set) {
        try {
            var f = TypeSourceUnionDynamic.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(this, set);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "TypeSourceUnionDynamic.initSetReflectively: failed to write final 'set' field", e);
        }
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        if (active.size() == variants.size()) {
            return "union_dyn";
        }
        var sb = new StringBuilder("union_dyn(");
        var first = true;
        for (var i : active) {
            if (!first) sb.append("|");
            first = false;
            sb.append(variants.get(i).tag.constKeyToCanonical());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.SOURCE_UNION_CONST);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(identity)
                || key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_UNION_DYNAMIC_IDENTITY)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectSet.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectFieldUseDynamic.AspectId)
                || key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.target(DynUtil.renderSource(xx, loc, source)),
                EvalOutcome.ofValue(new ValueDynamic(source, this))));
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
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsSome())
            return EResult.ok(dynCast.requireValue("dyn").optUnwrap());
        return EResult.err(loc, "TypeSourceUnionDynamic::toDynamic: CodeValue has no dynamic code");
    }

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        for (var v : variants) {
            out.add(new AspectUnion.Variant(v.tag, v.jsTag, v.type));
        }
        return out;
    }

    private JavaInternalName variantInternal(int variantIndex) {
        return JavaInternalName.of(SourceUnionEmitter.variantBinaryNameFor(baseBinaryName, variantIndex)
                .replace('.', '/'));
    }

    @Override
    public EResult<Void> matchDynamicPrepare(ExprContext xx) {
        xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        return EResult.ok(null);
    }

    @Override
    public EResult<CodeDynamic> matchDynamicTest(QExprId loc, int variantIndex) {
        return EResult.ok(JavaBytecodeUtils.instanceOf(loc, variantInternal(variantIndex)));
    }

    @Override
    public EResult<CodeDynamic> matchDynamicExtract(ExprContext xx, QExprId loc, int variantIndex, Type variantType) {
        var internal = variantInternal(variantIndex);
        var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, variantType);
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(internal)));
        seq.add(JavaBytecodeUtils.accessField(loc, internal, SourceUnionEmitter.PAYLOAD_FIELD, payloadDesc));
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
        if (value.value().optIsNone()) {
            return EResult.err(loc, "set: assigned union value has no compile-time value");
        }
        var newOpt = unionOf(xx, value.value().optUnwrap().valueType());
        var out = DynUtil.dynamicSet(xx, loc, base, value);
        if (out.hasErrors()) return out;
        if (newOpt.optIsNone() || !newOpt.optUnwrap().identity.equals(identity)) {
            return EResult.err(loc, "set: assigned value is not a value of this union");
        }
        var selfDyn = MustCast.mustCast(ValueDynamic.class, base.value().optUnwrap());
        return switch (selfDyn.source()) {
            case ValueDynamicSource.Binding b -> {
                var replacement = new BindingDynamicBinding(b.key(), withActiveVariants(newOpt.optUnwrap().active));
                if (!b.setter().bindingSetterSet(xx, loc, replacement, false)) {
                    yield EResult.err(loc, "set: union binding is no longer in scope");
                }
                yield out;
            }
            case ValueDynamicSource.Expr e -> out;
            case ValueDynamicSource.Field f -> out;
        };
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(retype(cv, this));
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return EResult.ok(retype(cv, destType));
    }

    private static CodeValue retype(CodeValue cv, Type dest) {
        var vd = MustCast.mustCast(ValueDynamic.class, cv.value().optUnwrap());
        return new CodeValue(cv.code(), EvalOutcome.ofValue(new ValueDynamic(vd.source(), dest)));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var tu = MustCast.mustCast(TypeSourceUnionDynamic.class, self);
            var set = tu.set;
            if (set == null) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceUnionConst::exporterRootDelegateSelfExport: union not attached to a"
                                + " definition set");
            }
            var errors = new TList<ExprErr>();
            var artifact =
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, tu.artifactIdCell, () -> {
                        var defSetArtRes = Artifactable.exportArtifact(xx, loc, set);
                        defSetArtRes.drainInto(errors);
                        var defSetArt = defSetArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var variantsOut = new TList<ArtifactDataSourceUnionVariant>();
                        for (var v : tu.variants) {
                            var tArtRes = Artifactable.exportArtifact(xx, loc, v.type);
                            tArtRes.drainInto(errors);
                            var tArt = tArtRes.valueOpt().optUnwrapOr(nullArtifact());
                            variantsOut.add(new ArtifactDataSourceUnionVariant(v.jsTag, v.tag, tArt));
                        }
                        var body = new ArtifactDataSourceUnion(
                                defSetArt,
                                set.defSetId,
                                tu.identity,
                                variantsOut,
                                tu.baseBinaryName,
                                tu.compiled.baseBytecode,
                                tu.compiled.variantBytecode);
                        return makeBody.apply(ArtifactDataSourceUnionCodec.toValue(body));
                    });
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeTypeSourceUnionConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataSourceUnion body;
                        try {
                            body = ArtifactDataSourceUnionCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("TypeTypeSourceUnionConst data: failed to parse "
                                    + "ArtifactDataSourceUnion: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var tu = new TypeSourceUnionDynamic(body.identity(), body.baseBinaryName());
                        tu.artifactIdCell.set(idValue);
                        tu.compiled.baseBytecode = body.compiledBaseBytecode();
                        tu.compiled.variantBytecode = body.compiledVariantBytecode();
                        for (var v : body.variants()) {
                            tu.active.add(tu.variants.size());
                            tu.variants.add(new SourceUnionVariant(v.tag(), v.jsTag(), GenType.dynamicType()));
                        }
                        var deps = new TList<Artifact>();
                        deps.add(body.defSet());
                        for (var v : body.variants()) deps.add(v.type_());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var defSetAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, body.defSet());
                            var defSetVal =
                                    defSetAny.requireValue("TypeTypeSourceUnionConst::finalizer defSet artifact");
                            if (!(defSetVal instanceof DefinitionSet set)) {
                                throw new IllegalStateException(
                                        "TypeTypeSourceUnionConst finalizer: defSet artifact resolved to "
                                                + defSetVal.getClass());
                            }
                            tu.initSetReflectively(set);
                            var variantsIt = body.variants().iterator();
                            for (int i = 0; i < tu.variants.size(); i++) {
                                var v = tu.variants.get(i);
                                var artV = variantsIt.next();
                                var tRes =
                                        ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, artV.type_());
                                var tVal = tRes.requireValue("TypeTypeSourceUnionConst::finalizer variant type");
                                tu.variants.set(i, new SourceUnionVariant(v.tag, v.jsTag, tVal));
                            }
                            return tu;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.some(tu), deps, fin));
                    });
        }
    };

    private static Artifact nullArtifact() {
        return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
    }
}
