package compilerxyz.main.eval.value.string;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
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
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectSet;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.AspectStringDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectStringConst;
import compilerxyz.main.gen.GenAspectStringDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;

@BuiltinPath("types.string")
@BuiltinFull(uniqueId = 6145L)
public record TypeStringDynamic()
        implements Type,
                AspectInitialBindDynamic,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectLift,
                AspectSet,
                AspectFieldUseDynamic,
                AspectStackValueDynamic,
                AspectStringDynamic {

    private static final JavaInternalName JAVA_STRING = JavaInternalName.fromClass(String.class);
    private static final JavaMethodDescriptor DESC_TO_STR =
            JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(String.class), TList.of());

    private static final IMap<ConstKey, TypeStringMethodDynamic> METHOD_FIELDS = IMap.ofEntries(
            new Pair<>(new ConstKey.Str("toLowerCase"), new TypeStringMethodDynamic("toLowerCase")),
            new Pair<>(new ConstKey.Str("toUpperCase"), new TypeStringMethodDynamic("toUpperCase")));

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        return "dynamic string";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(String.class));
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_STRING_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectStringDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectSet.AspectId)
                || key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.STRING_DYNAMIC);
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
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dynamicConsumeToDynamic(loc, cv);
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var s = MustCast.mustCast(String.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeStringConst(), s));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
        return DynUtil.dynamicSet(xx, loc, base, value);
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.target(DynUtil.renderSource(xx, loc, source)),
                EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeStringDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var fieldTypeOpt = aspectFieldTypeOpt(xx, ck);
        if (fieldTypeOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on dynamic string");
        }
        if (self_.value().optIsNone() || !(self_.value().optUnwrap() instanceof ValueDynamic strMethodVd)) {
            return EResult.err(loc, "TypeStringDynamic::field: method receiver has no dynamic source");
        }
        return EResult.ok(new CodeValue(
                self_.code(), EvalOutcome.ofValue(new ValueDynamic(strMethodVd.source(), fieldTypeOpt.optUnwrap()))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return METHOD_FIELDS.keys();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return METHOD_FIELDS.getOpt(key).optMap(t -> t);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_STRING_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return GenAspectStringConst.getMaybe(xx, sourceType).optIsSome()
                || GenAspectStringDynamic.getMaybe(xx, sourceType).optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return GenAspectStringDynamic.getMaybe(xx, destType).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeStringDynamic::typeCastFrom: CodeValue has no value");
        }
        var sourceType = cv.value().optUnwrap().valueType();
        var stringConstOpt = GenAspectStringConst.getMaybe(xx, sourceType);
        if (stringConstOpt.optIsSome()) {
            String strVal =
                    stringConstOpt.optUnwrap().aspectStringConstGet(cv.value().optUnwrap());
            var seq = new JavaBytecodeSequence();
            var existingDynCast = CodeDynamic.castOpt(loc, cv.code());
            if (existingDynCast.hasErrors()) return existingDynCast.reerr();
            var existingDyn = existingDynCast.requireValue("existingDyn");
            if (existingDyn.optIsSome()) {
                seq.add(existingDyn.optUnwrap());
            }
            seq.add(JavaBytecodeUtils.literalString(loc, strVal));
            var result = new CodeDynamic(loc, seq);
            return EResult.ok(new CodeValue(Code.target(result), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
        }
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }
}
