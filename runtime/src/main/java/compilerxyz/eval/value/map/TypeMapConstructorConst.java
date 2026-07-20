package compilerxyz.eval.value.map;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectTuple;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.gen.GenAspectType;

@compilerxyz.annotations.BuiltinTypeId(7032L)
@compilerxyz.annotations.BuiltinPath("types.map")
public record TypeMapConstructorConst()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() { return GenTypeIds.TYPE_MAP_CONSTRUCTOR_CONST_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "map constructor";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeMapConstructorConst.class, compilerxyz.gen.GenTypeIds.TYPE_MAP_CONSTRUCTOR_CONST_TYPE_ID, "map_constructor_const");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(
          loc, "TypeMapConstructorConst::call: argument must have a compile-time value");
    }
    var argValue = argSome.value();
    var argMeta = argValue.valueType();
    var aspectTupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
    if (!(aspectTupleOpt instanceof Opt.Some<AspectTuple> tupleSome)) {
      return EResult.err(loc, "map() requires a tuple of (key_type, value_type)");
    }
    var elements = tupleSome.value().aspectTupleTakeElements(xx, loc, argValue);
    if (elements.size() != 2) {
      return EResult.err(
          loc,
          "map() requires exactly 2 type arguments (key, value), got " + elements.size());
    }
    var keyRes = elements.get(0);
    if (keyRes.hasErrors()) return keyRes.reerr();
    var keyCv = keyRes.requireValue("TypeMapConstructorConst::call key element");
    var valueRes = elements.get(1);
    if (valueRes.hasErrors()) return valueRes.reerr();
    var valueCv = valueRes.requireValue("TypeMapConstructorConst::call value element");
    if (!(keyCv.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeMapConstructorConst::call: key slot has no compile-time value");
    }
    if (!(valueCv.value() instanceof Opt.Some<Value> valueSome)) {
      return EResult.err(
          loc, "TypeMapConstructorConst::call: value slot has no compile-time value");
    }
    var keyValue = keySome.value();
    var valueValue = valueSome.value();
    var keyAtRes = GenAspectType.get(xx, loc, keyValue.valueType());
    if (keyAtRes.hasErrors()) return keyAtRes.reerr();
    var keyType =
        keyAtRes.requireValue("TypeMapConstructorConst::call key AspectType").aspectTypeType(keyValue);
    var valueAtRes = GenAspectType.get(xx, loc, valueValue.valueType());
    if (valueAtRes.hasErrors()) return valueAtRes.reerr();
    var valueType =
        valueAtRes.requireValue("TypeMapConstructorConst::call value AspectType").aspectTypeType(valueValue);
    keyType = compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, keyType);
    valueType = compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, valueType);
    Type mapType = new TypeMapDynamic(keyType, valueType);
    return EResult.ok(
        new CodeValue(Code.none(loc), Opt.some(compilerxyz.eval.value.object_.ValueObjectConst.ofType(mapType))));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return Opt.none();
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
