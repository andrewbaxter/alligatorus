package compilerxyz.main.eval.value.null_;

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
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectNull;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectNull;
import compilerxyz.main.gen.GenAspectPostDeserialize;
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
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinPath("types.null")
@BuiltinFull(uniqueId = 6128L)
public record TypeNullDynamic()
        implements Type,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectLift,
                AspectNull,
                AspectStackValueDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public String typeDesc() {
        return "dynamic null";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_NULL_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectNull.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.NULL_DYNAMIC);
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) {
            return EResult.ok(dyn);
        }
        return EResult.ok(JavaBytecodeUtils.literalNull(loc));
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        return EResult.ok(new ValueObjectConst(new TypeNullConst(), null));
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_NULL_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return GenAspectNull.getMaybe(xx, sourceType).optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return GenAspectNull.getMaybe(xx, destType).optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY)
                        .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeNullDynamic::typeCastFrom: CodeValue has no value");
        }
        var sourceType = cv.value().optUnwrap().valueType();
        var nullRes = GenAspectNull.get(xx, loc, sourceType);
        if (nullRes.hasErrors()) {
            return nullRes.reerr();
        }
        if (cv.code().codeIsNone()) {
            var seq = new JavaBytecodeSequence();
            seq.add(JavaBytecodeUtils.literalNull(loc));
            return EResult.ok(new CodeValue(
                    Code.of(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
        }
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeNullDynamic)) {
                return EResult.err(
                        loc,
                        "TypeNullDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
            }
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueNull ignored -> EResult.ok(new TypeNullDynamic());
                case JsonValueBoolean ignored ->
                    EResult.err(
                            loc,
                            "TypeNullDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueNumber ignored ->
                    EResult.err(
                            loc,
                            "TypeNullDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueString ignored ->
                    EResult.err(
                            loc,
                            "TypeNullDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueArray ignored ->
                    EResult.err(
                            loc,
                            "TypeNullDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueObject ignored ->
                    EResult.err(
                            loc,
                            "TypeNullDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
            };
        }
    };

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }
}
