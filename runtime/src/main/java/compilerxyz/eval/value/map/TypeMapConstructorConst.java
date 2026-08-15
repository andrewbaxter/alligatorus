package compilerxyz.eval.value.map;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinPath;

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
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.gen.GenAspectType;
import compilerxyz.eval.dynamic.DynUtil;

@BuiltinPath("types.map")
@BuiltinLight(uniqueId = 6121L)
public record TypeMapConstructorConst()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {


  @Override
  public String typeDesc() {
    return "map constructor";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_CONSTRUCTOR_CONST_IDENTITY)) {
      return Opt.some(this);
    }
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
    Opt.Some<Value> argSome;
    switch (argument.value()) {
      case Opt.Some<Value> s -> argSome = s;
      case Opt.None<Value> n -> { return EResult.err(
          loc, "TypeMapConstructorConst::call: argument must have a compile-time value"); }
    }
    var argValue = argSome.value();
    var argMeta = argValue.valueType();
    var aspectTupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
    Opt.Some<AspectTuple> tupleSome;
    switch (aspectTupleOpt) {
      case Opt.Some<AspectTuple> s -> tupleSome = s;
      case Opt.None<AspectTuple> n -> { return EResult.err(loc, "map() requires a tuple of (key_type, value_type)"); }
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
    Opt.Some<Value> keySome;
    switch (keyCv.value()) {
      case Opt.Some<Value> s -> keySome = s;
      case Opt.None<Value> n -> { return EResult.err(loc, "TypeMapConstructorConst::call: key slot has no compile-time value"); }
    }
    Opt.Some<Value> valueSome;
    switch (valueCv.value()) {
      case Opt.Some<Value> s -> valueSome = s;
      case Opt.None<Value> n -> { return EResult.err(
          loc, "TypeMapConstructorConst::call: value slot has no compile-time value"); }
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
    keyType = DynUtil.substituteBoxed(xx, keyType);
    valueType = DynUtil.substituteBoxed(xx, valueType);
    Type mapType = new TypeMapDynamic(keyType, valueType);
    return EResult.ok(
        new CodeValue(Code.concat(loc, self.code(), argument.code()), EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), mapType))));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
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
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_MAP_CONSTRUCTOR_CONST_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
