package compilerxyz.eval.value.constkey;

import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTrivialIdentityAspect;
import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.artifact.ExporterAny;
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
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStringConst;
import compilerxyz.gen.GenTrivialIdentityAspects;

@BuiltinTypeId(7014L)
@BuiltinTrivialIdentityAspect(6015L)
@BuiltinPath("types.constkey")
public record TypeConstkeyDynamic()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() { return GenTypeIds.TYPE_CONSTKEY_DYNAMIC_TYPE_ID; }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(compilerxyz.eval.ConstKey.class);
  }

  @Override
  public String typeDesc() {
    return "constkey";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeConstkeyDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_CONSTKEY_DYNAMIC_TYPE_ID, "constkey_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
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
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return GenAspectStringConst.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeConstkeyDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    var stringConstOpt = GenAspectStringConst.getMaybe(xx, sourceType);
    if (!(stringConstOpt instanceof Opt.Some<AspectStringConst> stringConstSome)) {
      return EResult.err(loc, "Cannot convert " + sourceType.typeDesc() + " to constkey");
    }
    var s = stringConstSome.value().aspectStringConstGet(some.value());
    var canonical = new ConstKey.Str(s).toCanonical();
    var literal = JavaBytecodeUtils.literalString(loc, canonical);
    return EResult.ok(
        new CodeValue(
            Code.of(loc, literal),
            Opt.some(ValueDynamic.stack(xx, new TypeConstkeyDynamic()))));
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeConstkeyDynamic());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(new TypeConstkeyDynamic()));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY).optIsSome();
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY).optIsSome()) {
      return EResult.ok(cv);
    }
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
