package compilerxyz.eval.value.bool_;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
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
import compilerxyz.gen.AspectBoolConst;
import compilerxyz.gen.AspectBoolDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectDynamicUnboxed;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeBoolDynamicBox()
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectDynamicUnboxed,
        AspectInitialBind,
        AspectStackValueDynamic {

  @Override
  public String typeDesc() {
    return "dynamic bool";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JavaInternalName.of("java/lang/Boolean"));
  }

  @Override
  public Type unboxedForm() {
    return new TypeBoolDynamic();
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.BoolDynamicBox();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectDynamicUnboxed.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectStackValueDynamic.ID)
        || key.equals(AspectInitialBind.ID)) {
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
  public EResult<Opt<TargetBindSub>> bindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return AspectBoolConst.getMaybe(xx, sourceType).optIsSome()
        || AspectBoolDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return AspectBoolDynamic.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeBoolDynamicBox::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    var boolConstOpt = AspectBoolConst.getMaybe(xx, sourceType);
    var seq = new JavaBytecodeSequence();
    var existingDyn = cv.code().dynamic();
    if (existingDyn != null) {
      seq.add(existingDyn);
    }
    if (boolConstOpt instanceof Opt.Some<AspectBoolConst> boolConstSome) {
      boolean boolVal = boolConstSome.value().get(some.value());
      seq.add(JavaBytecodeUtils.literalBool(loc, boolVal));
      seq.add(valueOfBoolean(loc));
    } else if (sourceType instanceof TypeBoolDynamic) {
      seq.add(valueOfBoolean(loc));
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

  private static CodeDynamic valueOfBoolean(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        JavaInternalName.of("java/lang/Boolean"),
        "valueOf",
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromInternalName(JavaInternalName.of("java/lang/Boolean")),
            TList.of(JavaDataDescriptor.DESC_BOOL)));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
