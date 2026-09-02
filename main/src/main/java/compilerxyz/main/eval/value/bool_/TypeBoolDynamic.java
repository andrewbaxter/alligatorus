package compilerxyz.main.eval.value.bool_;

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
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectBoolDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectDynamicBoxed;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectBoolConst;
import compilerxyz.main.gen.GenAspectBoolDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectDynamicBoxed;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
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

@BuiltinPath("types.bool")
@BuiltinFull(uniqueId = 6103L)
public record TypeBoolDynamic()
        implements Type,
                AspectBind,
                AspectBindDynamic,
                AspectBoolDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectDynamicBoxed,
                AspectElementUseDynamicBind,
                AspectFieldUseDynamic,
                AspectInitialBind,
                AspectLift,
                AspectStackValueDynamic {

    @Override
    public String typeDesc() {
        return "dynamic bool";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.DESC_BOOL;
    }

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeBoolDynamicBox();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_BOOL_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectBoolDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectDynamicBoxed.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectFieldUseDynamic.AspectId)
                || key.equals(GenAspectLift.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.BOOL_DYNAMIC);
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.of(DynUtil.renderSource(xx, loc, source)), EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dynamicConsumeToDynamic(loc, cv);
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        if (externalHostValue.optIsNone()) {
            return EResult.err(loc, "TypeBoolDynamic::lift: expected java.lang.Boolean, got null");
        }
        var rawHostValue = externalHostValue.optUnwrap();
        if (!(rawHostValue instanceof Boolean b)) {
            return EResult.err(
                    loc,
                    "TypeBoolDynamic::lift: expected java.lang.Boolean, got "
                            + rawHostValue.getClass().getName());
        }
        return EResult.ok(new ValueObjectConst(new TypeBoolConst(), b));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_BOOL_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return GenAspectBoolConst.getMaybe(xx, sourceType).optIsSome()
                || GenAspectBoolDynamic.getMaybe(xx, sourceType).optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return GenAspectBoolDynamic.getMaybe(xx, destType).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeBoolDynamic::typeCastFrom: CodeValue has no value");
        }
        var sourceType = cv.value().optUnwrap().valueType();
        var boolConstOpt = GenAspectBoolConst.getMaybe(xx, sourceType);
        if (boolConstOpt.optIsSome()) {
            boolean boolVal =
                    boolConstOpt.optUnwrap().aspectBoolConstGet(cv.value().optUnwrap());
            var seq = new JavaBytecodeSequence();
            var existingDyn = cv.code().codeDynamic();
            if (existingDyn != null) {
                seq.add(existingDyn);
            }
            seq.add(JavaBytecodeUtils.literalBool(loc, boolVal));
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
            if (!(self instanceof TypeBoolDynamic)) {
                return EResult.err(
                        loc,
                        "TypeBoolDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
            }
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueNull n -> EResult.ok(new TypeBoolDynamic());
                case JsonValueBoolean b ->
                    EResult.err(
                            loc,
                            "TypeBoolDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueNumber n ->
                    EResult.err(
                            loc,
                            "TypeBoolDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueString s ->
                    EResult.err(
                            loc,
                            "TypeBoolDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueArray a ->
                    EResult.err(
                            loc,
                            "TypeBoolDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueObject o ->
                    EResult.err(
                            loc,
                            "TypeBoolDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
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
