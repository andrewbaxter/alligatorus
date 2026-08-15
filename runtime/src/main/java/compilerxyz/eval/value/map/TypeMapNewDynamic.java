package compilerxyz.eval.value.map;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.dynamic.DynUtil;

@BuiltinLight(uniqueId = 6125L)
public record TypeMapNewDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {


  @Override
  public String typeDesc() {
    return "map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ").new";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_NEW_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var instanceType = new TypeMapDynamic(DynUtil.substituteBoxed(xx, keyType), DynUtil.substituteBoxed(xx, valueType));
    if (argument.value().optIsNone()) {
      return EResult.err(loc, "TypeMapNewDynamic::call: argument has no compile-time value");
    }
    var argType = argument.value().optUnwrap().valueType();
    var argTupleOpt = GenAspectTuple.getMaybe(xx, argType);
    if (argTupleOpt.optIsSome() && argTupleOpt.optUnwrap().aspectTupleElementTypes().isEmpty()) {
      var retStackRes = GenAspectStackValueDynamic.get(xx, loc, instanceType);
      if (retStackRes.hasErrors()) return retStackRes.reerr();

      var torderedMapIntname = JavaInternalName.fromClass(TOrderedMap.class);
      var instantiateMap =
          JavaBytecodeUtils.instantiate(
              loc,
              torderedMapIntname,
              JavaMethodDescriptor.fromConstructorParts(TList.of()),
              new CodeDynamic(loc, new JavaBytecodeSequence()));
      var selfDyn = self.code().codeDynamic();
      CodeDynamic newMap;
      if (selfDyn == null) {
        newMap = instantiateMap;
      } else {
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(instantiateMap);
        newMap = new CodeDynamic(loc, seq);
      }
      return EResult.ok(
          retStackRes.requireValue("AspectStackValueDynamic.get").aspectStackValueDynamicToValue(xx, loc, newMap));
    }
    return Eval.typecheckConvert(xx, loc, instanceType, argument);
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
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_MAP_NEW_DYNAMIC_IDENTITY, TypeMapNewDynamic.class);
    return oOpt.optIsSome() && keyType.typeIsEqual(xx, oOpt.optUnwrap().keyType) && valueType.typeIsEqual(xx, oOpt.optUnwrap().valueType);
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
