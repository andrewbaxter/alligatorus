package compilerxyz.main.eval.value.ubyte_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectDynamicUnboxed;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.AspectSet;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectDynamicUnboxed;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6182L)
public record TypeUbyteDynamicBox()
        implements Type,
                AspectInitialBindDynamic,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectDynamicUnboxed,
                AspectField,
                AspectSet,
                AspectStackValueDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        return "dynamic ubyte";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Byte.class));
    }

    @Override
    public Type aspectDynamicUnboxedUnboxedForm() {
        return new TypeUbyteDynamic();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_UBYTE_DYNAMIC_BOX_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectDynamicUnboxed.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectSet.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
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
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
        return DynUtil.dynamicSet(xx, loc, base, value);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UBYTE_DYNAMIC_BOX_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return Numerics.isNumeric(xx, sourceType);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return Numerics.isNumeric(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        var unboxedCast = Numerics.castTo(xx, loc, cv, NumericKind.UBYTE, new TypeUbyteDynamic());
        if (unboxedCast.hasErrors()) return unboxedCast.reerr();
        var castCv = unboxedCast.requireValue("TypeUbyteDynamicBox::typeCastFrom cast");
        var seq = new JavaBytecodeSequence();
        var dynCast = CodeDynamic.castOpt(loc, castCv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        var dyn = dynCast.requireValue("dyn");
        if (dyn.optIsSome()) seq.add(dyn.optUnwrap());
        seq.add(Numerics.box(loc, NumericKind.UBYTE));
        return EResult.ok(new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var selfDynRes = CodeDynamic.castRequired(
                loc, cv.code(), "TypeUbyteDynamicBox::typeCastTo: receiver has no runtime code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(Numerics.unbox(loc, NumericKind.UBYTE));
        var unboxed = new CodeValue(
                Code.target(new CodeDynamic(loc, seq)),
                EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeUbyteDynamic())));
        var kkOpt = Numerics.kindOf(xx, destType);
        if (kkOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, unboxed, kkOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeUbyteDynamicBox::typeCastTo: destination is not numeric");
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        var unboxed = new TypeUbyteDynamic();
        var selfDynRes =
                CodeDynamic.castRequired(loc, self_.code(), "TypeUbyteDynamicBox::field: receiver has no runtime code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(Numerics.unbox(loc, NumericKind.UBYTE));
        var unboxedSelf = new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, unboxed)));
        return unboxed.aspectFieldField(xx, loc, unboxedSelf, key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TypeUbyteDynamic().aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return new TypeUbyteDynamic().aspectFieldTypeOpt(xx, key);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
