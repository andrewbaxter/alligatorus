package compilerxyz.eval.value.constkey;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
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
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import compilerxyz.eval.dynamic.DynUtil;

public record TypeConstkeyDynamic()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectInitialBind,
        ExporterAny {

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "constkey";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.ConstkeyDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
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
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return AspectStringConst.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeConstkeyDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    var stringConstOpt = AspectStringConst.getMaybe(xx, sourceType);
    if (!(stringConstOpt instanceof Opt.Some<AspectStringConst> stringConstSome)) {
      return EResult.err(loc, "Cannot convert " + sourceType.typeDesc() + " to constkey");
    }
    var s = stringConstSome.value().get(some.value());
    var canonical = new ConstKey.Str(s).toCanonical();
    var literal = JavaBytecodeUtils.literalString(loc, canonical);
    return EResult.ok(
        new CodeValue(
            Code.of(loc, literal),
            Opt.some(ValueDynamic.stack(new TypeConstkeyDynamic()))));
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeConstkeyDynamic());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new TypeConstkeyDynamic());
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
