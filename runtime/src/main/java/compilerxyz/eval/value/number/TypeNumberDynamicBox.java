package compilerxyz.eval.value.number;

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
import compilerxyz.gen.AspectNumberConst;
import compilerxyz.gen.AspectNumberDynamic;
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
import compilerxyz.gen.GenAspectNumberConst;
import compilerxyz.gen.GenAspectNumberDynamic;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.gen.GenAspectStackValueDynamic;

@compilerxyz.annotations.BuiltinTypeId(7003L)
public record TypeNumberDynamicBox()
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
  public TypeId hasTypeIdTypeId() { return GenTypeIds.TYPE_NUMBER_DYNAMIC_BOX_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "dynamic number";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JavaInternalName.fromClass(Double.class));
  }

  @Override
  public Type aspectDynamicUnboxedUnboxedForm() {
    return new TypeNumberDynamic();
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeNumberDynamicBox.class, compilerxyz.gen.GenTypeIds.TYPE_NUMBER_DYNAMIC_BOX_TYPE_ID, "number_dynamic_box");

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
    return GenAspectNumberConst.getMaybe(xx, sourceType).optIsSome()
        || GenAspectNumberDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return GenAspectNumberDynamic.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeNumberDynamicBox::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    var numConstOpt = GenAspectNumberConst.getMaybe(xx, sourceType);
    var seq = new JavaBytecodeSequence();
    var existingDyn = cv.code().dynamic();
    if (existingDyn != null) {
      seq.add(existingDyn);
    }
    if (numConstOpt instanceof Opt.Some<AspectNumberConst> numConstSome) {
      double numVal = numConstSome.value().aspectNumberConstGet(some.value());
      seq.add(JavaBytecodeUtils.literalDouble(loc, numVal));
      seq.add(valueOfDouble(loc));
    } else if (sourceType instanceof TypeNumberDynamic) {
      seq.add(valueOfDouble(loc));
    }
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            Opt.some(ValueDynamic.stack(xx, this))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(ValueDynamic.stack(xx, this))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var unboxed = new TypeNumberDynamic();
    var selfDyn = self_.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeNumberDynamicBox::field: receiver has no runtime code");
    }
    var seq = new JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(doubleValueUnbox(loc));
    var unboxedSelf =
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            Opt.some(ValueDynamic.stack(xx, unboxed)));
    return unboxed.aspectFieldField(xx, loc, unboxedSelf, key);
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TypeNumberDynamic().aspectFieldKeys(xx);
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeNumberDynamic().aspectFieldTypeOpt(xx, key);
  }

  private static CodeDynamic doubleValueUnbox(QExprId loc) {
    return JavaBytecodeUtils.callMethod(
        loc,
        JavaInternalName.fromClass(Double.class),
        "doubleValue",
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_DOUBLE, TList.of()));
  }

  private static CodeDynamic valueOfDouble(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        JavaInternalName.fromClass(Double.class),
        "valueOf",
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Double.class)),
            TList.of(JavaDataDescriptor.DESC_DOUBLE)));
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
