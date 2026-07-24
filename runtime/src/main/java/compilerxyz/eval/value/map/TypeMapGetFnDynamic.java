package compilerxyz.eval.value.map;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.TList;
import java.util.Map;

@BuiltinTypeId(7034L)
public record TypeMapGetFnDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectFunction,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() { return new TypeId.Composite(GenTypeIds.TYPE_MAP_GET_FN_DYNAMIC_TYPE_ID, TList.of(keyType.hasTypeIdTypeId(), valueType.hasTypeIdTypeId())); }


  @Override
  public String typeDesc() {
    return "map get fn";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeMapGetFnDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_MAP_GET_FN_DYNAMIC_TYPE_ID, "map_get_fn_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenAspectFunction.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var keyConvertedRes = Eval.typecheckConvert(xx, loc, keyType, argument);
    if (keyConvertedRes.hasErrors()) return keyConvertedRes.reerr();
    var keyConverted = keyConvertedRes.requireValue("get key converted");
    var keyConsumeRes = GenAspectConsumeDynamic.get(xx, loc, keyType);
    if (keyConsumeRes.hasErrors()) return keyConsumeRes.reerr();
    var keyDynRes =
        keyConsumeRes
            .requireValue("AspectConsumeDynamic.get key")
            .aspectConsumeDynamicToDynamic(xx, loc, keyConverted);
    if (keyDynRes.hasErrors()) return keyDynRes.reerr();
    var keyDyn = keyDynRes.requireValue("toDynamic key");

    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeMapGetFnDynamic::call: self has no dynamic code");
    }

    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, valueType);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var mapName = JavaInternalName.fromClass(Map.class);
    var getDesc =
        JavaMethodDescriptor.fromParts(
            JvmConstants.DESC_OBJECT, TList.of(JvmConstants.DESC_OBJECT));

    var seq = new JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(keyDyn);
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "get", getDesc));
    var valueConsumeRes = GenAspectConsumeDynamic.get(xx, loc, valueType);
    if (valueConsumeRes.hasErrors()) return valueConsumeRes.reerr();
    var valueDesc =
        valueConsumeRes
            .requireValue("AspectConsumeDynamic.get value")
            .aspectConsumeDynamicJvmClass(xx);
    if (valueDesc.value().startsWith("L") && !valueDesc.value().equals("Ljava/lang/Object;")) {
      seq.add(JavaBytecodeUtils.cast(loc, valueDesc));
    }
    return EResult.ok(
        retStackRes
            .requireValue("AspectStackValueDynamic.get")
            .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return Opt.none();
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
  public boolean typeIsEqual(compilerxyz.eval.Type other) {
    return other instanceof TypeMapGetFnDynamic o && keyType.typeIsEqual(o.keyType) && valueType.typeIsEqual(o.valueType);
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
