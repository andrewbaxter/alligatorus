package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Counter;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
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
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectSet;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6021L)
public final class TypeSourceUnionDynamic
        implements Type,
                AspectFieldUseDynamic,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectSet,
                AspectStackValueDynamic,
                AspectUnion,
                MatchDynamic,
                ExporterAny {

    @SuppressWarnings("NullAway.Init")
    public final DefinitionSet set;

    public int unionIdx;
    public final Unique identity;
    public final Counter nextTagId;
    public final TList<SourceUnionVariant> variants;
    public final TList<Integer> active;
    public final ArtifactIdHolder artifactIdCell;
    public final SourceUnionCompiled compiled;

    public TypeSourceUnionDynamic(DefinitionSet set, Unique identity) {
        this.set = set;
        this.identity = identity;
        this.nextTagId = new Counter();
        this.variants = new TList<>();
        this.active = new TList<>();
        this.artifactIdCell = new ArtifactIdHolder();
        this.compiled = new SourceUnionCompiled();
    }

    @SuppressWarnings("NullAway")
    public TypeSourceUnionDynamic(Unique identity) {
        this.set = (DefinitionSet) null;
        this.identity = identity;
        this.nextTagId = new Counter();
        this.variants = new TList<>();
        this.active = new TList<>();
        this.artifactIdCell = new ArtifactIdHolder();
        this.compiled = new SourceUnionCompiled();
    }

    private TypeSourceUnionDynamic(TypeSourceUnionDynamic base, TList<Integer> active) {
        this.set = base.set;
        this.unionIdx = base.unionIdx;
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
        if (dynOpt.optIsSome()) return dynOpt;
        var constOpt = AspectLookup.getMaybe(
                xx, t, GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_CONST_IDENTITY, TypeTypeSourceUnionConst.class);
        if (constOpt.optIsSome()) return Opt.some(constOpt.optUnwrap().type_());
        return Opt.none();
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
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return new TypeTypeSourceUnionConst(this).artifactableExportArtifact(xx, loc);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(identity)
                || key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_UNION_DYNAMIC_IDENTITY)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
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
                Code.of(loc, DynUtil.renderSource(xx, loc, source)),
                EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(xx, loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) return EResult.ok(dyn);
        return EResult.err(loc, "TypeSourceUnionDynamic::toDynamic: CodeValue has no dynamic code");
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        for (var v : variants) {
            out.add(new AspectUnion.Variant(v.tag, v.jsTag, v.type));
        }
        return out;
    }

    private JavaInternalName variantInternal(QExprId loc, int variantIndex) {
        return JavaInternalName.of(SourceUnionEmitter.variantBinaryNameFor(set.defSetId, unionIdx, variantIndex)
                .replace('.', '/'));
    }

    @Override
    public EResult<Void> matchDynamicPrepare(ExprContext xx, QExprId loc) {
        xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        return EResult.ok(null);
    }

    @Override
    public EResult<CodeDynamic> matchDynamicTest(ExprContext xx, QExprId loc, int variantIndex) {
        return EResult.ok(JavaBytecodeUtils.instanceOf(loc, variantInternal(loc, variantIndex)));
    }

    @Override
    public EResult<CodeDynamic> matchDynamicExtract(ExprContext xx, QExprId loc, int variantIndex, Type variantType) {
        var internal = variantInternal(loc, variantIndex);
        var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, variantType);
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(internal)));
        seq.add(JavaBytecodeUtils.accessField(loc, internal, SourceUnionEmitter.PAYLOAD_FIELD, payloadDesc));
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(new TypeTypeSourceUnionConst(this), this));
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(xx, loc, cv);
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
                if (b.setter().optIsNone()) {
                    yield EResult.err(loc, "set: union binding is not settable here");
                }
                var replacement = new BindingDynamicBinding(b.key(), withActiveVariants(newOpt.optUnwrap().active));
                if (!b.setter().optUnwrap().bindingSetterSet(xx, loc, replacement, false)) {
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

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }
}
