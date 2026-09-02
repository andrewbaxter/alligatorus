package compilerxyz.main.eval.value.code;

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
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.MatchDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectUnion;
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
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;
import org.objectweb.asm.Opcodes;

@BuiltinPath("types.code")
@BuiltinFull(uniqueId = 6300L)
public record TypeCodeDynamic()
        implements Type,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectLift,
                AspectStackValueDynamic,
                AspectUnion,
                MatchDynamic {

    private static final JavaInternalName CODE_INTNAME = JavaInternalName.fromClass(Code.class);

    @Override
    public String typeDesc() {
        return "code";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(Code.class);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_CODE_DYNAMIC_IDENTITY)
                || key.equals(GenTrivialIdentityAspects.CODE_IDENTITY)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        out.add(new AspectUnion.Variant(new ConstKey.Str("none"), "none", new TypeNullDynamic()));
        out.add(new AspectUnion.Variant(new ConstKey.Str("target"), "target", new TypeCodeDynamic()));
        return out;
    }

    @Override
    public EResult<Void> matchDynamicPrepare(ExprContext xx) {
        return EResult.ok(null);
    }

    @Override
    public EResult<CodeDynamic> matchDynamicTest(QExprId loc, int variantIndex) {
        var desc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of());
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, CODE_INTNAME, "codeIsNone", desc));
        if (variantIndex == 1) {
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_1));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.IXOR));
        }
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    @Override
    public EResult<CodeDynamic> matchDynamicExtract(ExprContext xx, QExprId loc, int variantIndex, Type variantType) {
        var seq = new JavaBytecodeSequence();
        if (variantIndex == 0) {
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
        }
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) return EResult.ok(dyn);
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeCodeDynamic::toDynamic: CodeValue has no value");
        }
        if (!(cv.value().optUnwrap() instanceof ValueObjectConst voc) || !(voc.data() instanceof Code codeObj)) {
            return EResult.err(loc, "TypeCodeDynamic::toDynamic: value is not a compile-time Code");
        }
        var key = new JavaBytecodeBindingKey();
        var id = new BindingId.Local(key);
        xx.vx.externals.add(new EvalContext.ExternalEntry(key, this, new External(Code.class, codeObj)));
        return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
    }

    @Override
    public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        if (externalHostValue.optIsNone() || !(externalHostValue.optUnwrap() instanceof Code codeObj)) {
            return EResult.err(loc, "TypeCodeDynamic::lift: expected host Code");
        }
        return EResult.ok(new ValueObjectConst(new TypeCodeDynamic(), codeObj));
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.CODE_DYNAMIC);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueNull ignored -> EResult.ok(new TypeCodeDynamic());
                case JsonValueBoolean ignored ->
                    EResult.err(
                            loc,
                            "TypeCodeDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueNumber ignored ->
                    EResult.err(
                            loc,
                            "TypeCodeDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueString ignored ->
                    EResult.err(
                            loc,
                            "TypeCodeDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueArray ignored ->
                    EResult.err(
                            loc,
                            "TypeCodeDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueObject ignored ->
                    EResult.err(
                            loc,
                            "TypeCodeDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
            };
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CODE_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType
                .typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
}
