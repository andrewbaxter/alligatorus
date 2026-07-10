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
import compilerxyz.eval.Value;
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
import compilerxyz.eval.value.TypeRoot;
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
  public TypeId typeId() { return GenTypeIds.TYPE_NUMBER_DYNAMIC_BOX_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "dynamic number";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JavaInternalName.of("java/lang/Double"));
  }

  @Override
  public Type aspectDynamicUnboxedUnboxedForm() {
    return new TypeNumberDynamic();
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.NumberDynamicBox();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectDynamicUnboxed.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectStackValueDynamic.ID)
        || key.equals(AspectSet.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectElementUseDynamicBind.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
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
  public CodeValue aspectStackValueDynamicToValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public EResult<CodeValue> set(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return AspectNumberConst.getMaybe(xx, sourceType).optIsSome()
        || AspectNumberDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return AspectNumberDynamic.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeNumberDynamicBox::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    var numConstOpt = AspectNumberConst.getMaybe(xx, sourceType);
    var seq = new JavaBytecodeSequence();
    var existingDyn = cv.code().dynamic();
    if (existingDyn != null) {
      seq.add(existingDyn);
    }
    if (numConstOpt instanceof Opt.Some<AspectNumberConst> numConstSome) {
      double numVal = numConstSome.value().get(some.value());
      seq.add(JavaBytecodeUtils.literalDouble(loc, numVal));
      seq.add(valueOfDouble(loc));
    } else if (sourceType instanceof TypeNumberDynamic) {
      seq.add(valueOfDouble(loc));
    }
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            Opt.some(ValueDynamic.stack(this))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(ValueDynamic.stack(this))));
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
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
            Opt.some(ValueDynamic.stack(unboxed)));
    return unboxed.field(xx, loc, unboxedSelf, key);
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TypeNumberDynamic().fieldKeys(xx);
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeNumberDynamic().fieldTypeOpt(xx, key);
  }

  private static CodeDynamic doubleValueUnbox(QExprId loc) {
    return JavaBytecodeUtils.callMethod(
        loc,
        JavaInternalName.of("java/lang/Double"),
        "doubleValue",
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_DOUBLE, TList.of()));
  }

  private static CodeDynamic valueOfDouble(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        JavaInternalName.of("java/lang/Double"),
        "valueOf",
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromInternalName(JavaInternalName.of("java/lang/Double")),
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
