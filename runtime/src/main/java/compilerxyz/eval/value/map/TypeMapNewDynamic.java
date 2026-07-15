package compilerxyz.eval.value.map;

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
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

@compilerxyz.annotations.BuiltinTypeId(7031L)
public record TypeMapNewDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() { return new TypeId.Composite(GenTypeIds.TYPE_MAP_NEW_DYNAMIC_TYPE_ID, TList.of(keyType.hasTypeIdTypeId(), valueType.hasTypeIdTypeId())); }


  @Override
  public String typeDesc() {
    return "map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ").new";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeMapNewDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_MAP_NEW_DYNAMIC_TYPE_ID, "map_new_dynamic");

  @Override
  public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID) || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var instanceType = new TypeMapDynamic(compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, keyType), compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, valueType));
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "TypeMapNewDynamic::call: argument has no compile-time value");
    }
    var argType = argSome.value().valueType();
    var argTupleOpt = AspectTuple.getMaybe(xx, argType);
    if (argTupleOpt instanceof Opt.Some<AspectTuple> argTupleSome
        && argTupleSome.value().aspectTupleElementTypes().isEmpty()) {
      var retStackRes = AspectStackValueDynamic.get(xx, loc, instanceType);
      if (retStackRes.hasErrors()) return retStackRes.reerr();

      var torderedMapIntname = JavaInternalName.fromClass(TOrderedMap.class);
      var newMap =
          JavaBytecodeUtils.instantiate(
              loc,
              torderedMapIntname,
              JavaMethodDescriptor.fromConstructorParts(TList.of()),
              new CodeDynamic(loc, new JavaBytecodeSequence()));
      return EResult.ok(
          retStackRes.requireValue("AspectStackValueDynamic.get").aspectStackValueDynamicToValue(xx, loc, newMap));
    }
    return Eval.typecheckConvert(xx, loc, instanceType, argument);
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
    return EResult.ok(this);
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
