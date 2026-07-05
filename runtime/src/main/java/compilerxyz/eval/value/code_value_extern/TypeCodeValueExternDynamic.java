package compilerxyz.eval.value.code_value_extern;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
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
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.code.TypeCodeStructDynamic;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.dynamic.DynUtil;

public record TypeCodeValueExternDynamic()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "CodeValueExtern";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.CodeValueExternDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.CODE_VALUE_EXTERN_TYPE_IDENTITY)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
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
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeCodeValueExternDynamic::toDynamic: CodeValue has no dynamic code");
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
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    return EResult.err(
        loc,
        "TypeCodeValueExternDynamic::field: CVE_GET_CODE_ENUM_FN_SCOPE_ID /"
            + " CVE_GET_VALUE_FN_SCOPE_ID emission (deferred)");
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    return switch (s.value()) {
      case "code" -> Opt.some(new TypeCodeStructDynamic());

      default -> Opt.none();
    };
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, GenIdentities.CODE_VALUE_EXTERN_TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeCodeValueExternDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    if (sourceType.typeAspect(xx, GenIdentities.CODE_VALUE_EXTERN_TYPE_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(ValueDynamic.stack(this))));
    }
    return EResult.err(loc, "Cannot convert to CodeValueExtern");
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
