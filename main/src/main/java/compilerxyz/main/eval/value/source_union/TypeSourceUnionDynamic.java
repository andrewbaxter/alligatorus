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
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectConsumeDynamicObject;
import compilerxyz.main.eval.aspect.AspectDropDynamicLightDefault;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.MatchDynamic;
import compilerxyz.main.eval.dynamic.SourceUnionEmitter;
import compilerxyz.main.eval.value.ValueDynamic;
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
public final class TypeSourceUnionDynamic implements Aspect, Type, MatchDynamic {

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
            return Opt.some(new AspectInitialBindDynamicSourceUnionDynamic(this));
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(new AspectUnionSourceUnionDynamic(this));
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicSourceUnionDynamic(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(new AspectSetSourceUnionDynamic(this));
        }
        if (key.equals(GenAspectDropDynamicLight.AspectId)) {
            return Opt.some(AspectDropDynamicLightDefault.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicObject.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicSourceUnionDynamic(this));
        }
        if (key.equals(identity)
                || key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_UNION_DYNAMIC_IDENTITY)
                || key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
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
                        for (var v : body.variants()) deps.add(v.type());
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
                                        ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, artV.type());
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
