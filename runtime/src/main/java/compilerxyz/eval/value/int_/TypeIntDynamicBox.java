package compilerxyz.eval.value.int_;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectDynamicUnboxed;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectInt;
import compilerxyz.gen.AspectIntDynamic;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.AspectSet;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectDynamicUnboxed;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectInt;
import compilerxyz.gen.GenAspectIntDynamic;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.gen.GenAspectStackValueDynamic;

@BuiltinTypeId(7003L)
public record TypeIntDynamicBox()
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectDynamicUnboxed,
        AspectField,
        AspectInitialBind,
        AspectElementUseDynamicBind,
        AspectSet,
        AspectStackValueDynamic {

  @Override
  public TypeId hasTypeIdTypeId() { return GenTypeIds.TYPE_INT_DYNAMIC_BOX_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "dynamic int";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JavaInternalName.fromClass(Long.class));
  }

  @Override
  public Type aspectDynamicUnboxedUnboxedForm() {
    return new TypeIntDynamic();
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeIntDynamicBox.class, compilerxyz.gen.GenTypeIds.TYPE_INT_DYNAMIC_BOX_TYPE_ID, "int_dynamic_box");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectDynamicUnboxed.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(GenAspectSet.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)) {
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
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this);
  }

  @Override
  public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return GenAspectInt.getMaybe(xx, sourceType).optIsSome()
        || GenAspectIntDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return GenAspectIntDynamic.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public boolean typeIsEqual(compilerxyz.eval.Type other) {
    return other instanceof TypeIntDynamicBox;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeIntDynamicBox::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    var numConstOpt = GenAspectInt.getMaybe(xx, sourceType);
    var seq = new JavaBytecodeSequence();
    var existingDyn = cv.code().dynamic();
    if (existingDyn != null) {
      seq.add(existingDyn);
    }
    if (numConstOpt instanceof Opt.Some<AspectInt> numConstSome) {
      long numVal = numConstSome.value().aspectIntGet(some.value());
      seq.add(JavaBytecodeUtils.literalLong(loc, numVal));
      seq.add(valueOfLong(loc));
    } else if (sourceType instanceof TypeIntDynamic) {
      seq.add(valueOfLong(loc));
    }
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var unboxed = new TypeIntDynamic();
    var selfDyn = self_.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeIntDynamicBox::field: receiver has no runtime code");
    }
    var seq = new JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(longValueUnbox(loc));
    var unboxedSelf =
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(xx, unboxed)));
    return unboxed.aspectFieldField(xx, loc, unboxedSelf, key);
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TypeIntDynamic().aspectFieldKeys(xx);
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeIntDynamic().aspectFieldTypeOpt(xx, key);
  }

  private static CodeDynamic longValueUnbox(QExprId loc) {
    return JavaBytecodeUtils.callMethod(
        loc,
        JavaInternalName.fromClass(Long.class),
        "longValue",
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_LONG, TList.of()));
  }

  private static CodeDynamic valueOfLong(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        JavaInternalName.fromClass(Long.class),
        "valueOf",
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Long.class)),
            TList.of(JavaDataDescriptor.DESC_LONG)));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }


  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return aspectInitialBindInitialBind(dynamicKey);
  }

}
