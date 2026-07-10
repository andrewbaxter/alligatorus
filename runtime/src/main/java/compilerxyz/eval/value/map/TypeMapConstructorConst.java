package compilerxyz.eval.value.map;

import compilerxyz.artifact.ExporterAny;
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
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectTuple;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeMapConstructorConst()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_MAP_CONSTRUCTOR_CONST_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "map constructor";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.MapConstructorConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID) || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(
          loc, "TypeMapConstructorConst::call: argument must have a compile-time value");
    }
    var argValue = argSome.value();
    var argMeta = argValue.valueTypeOf();
    var aspectTupleOpt = AspectTuple.getMaybe(xx, argMeta);
    if (!(aspectTupleOpt instanceof Opt.Some<AspectTuple> tupleSome)) {
      return EResult.err(loc, "map() requires a tuple of (key_type, value_type)");
    }
    var elements = tupleSome.value().takeElements(xx, loc, argValue);
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
    var keyAtRes = AspectType.get(xx, loc, keyValue.valueTypeOf());
    if (keyAtRes.hasErrors()) return keyAtRes.reerr();
    var keyType =
        keyAtRes.requireValue("TypeMapConstructorConst::call key AspectType").type_(keyValue);
    var valueAtRes = AspectType.get(xx, loc, valueValue.valueTypeOf());
    if (valueAtRes.hasErrors()) return valueAtRes.reerr();
    var valueType =
        valueAtRes.requireValue("TypeMapConstructorConst::call value AspectType").type_(valueValue);
    keyType = compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, keyType);
    valueType = compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, valueType);
    Type mapType = new TypeMapDynamic(keyType, valueType);
    return EResult.ok(
        new CodeValue(Code.none(loc), Opt.some(new ValueObjectConst(mapType))));
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
