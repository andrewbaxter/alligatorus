package compilerxyz.eval.value.eresult;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeEResultVariantConstructorDynamic(Type elementType, boolean isOk)
    implements
        Type,
        AspectFunction,
        ExporterAny {

  @Override
  public TypeId typeId() {
    return new TypeId.Composite(
        GenTypeIds.TYPE_ERESULT_VARIANT_CONSTRUCTOR_DYNAMIC_TYPE_ID,
        TList.of(elementType.typeId()));
  }

  @Override
  public String typeDesc() {
    return "eresult(" + elementType.typeDesc() + ")." + (isOk ? "ok" : "err");
  }

  @Override
  public Type valueType() {
    return new TypeRoot.EResultVariantConstructorDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var erb = new Erb(loc);
    Type resultType = new TypeEResultDynamic(elementType);
    if (isOk) {
      var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, elementType, argument));
      if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
      var consumeRes = compilerxyz.gen.AspectConsumeDynamic.get(xx, loc, elementType);
      if (consumeRes.hasErrors()) return consumeRes.reerr();
      var innerJsRes =
          consumeRes
              .requireValue("TypeEResultVariantConstructorDynamic::call ok consume")
              .aspectConsumeDynamicToDynamic(xx, loc, convertedOpt.get());
      if (innerJsRes.hasErrors()) return innerJsRes.reerr();
      var innerJs = innerJsRes.requireValue("TypeEResultVariantConstructorDynamic::call ok toDynamic");
      var okSeq = new JavaBytecodeSequence();
      okSeq.add(innerJs);
      okSeq.add(
          JavaBytecodeUtils.callStaticMethod(
              loc,
              JavaInternalName.of("compilerxyz/eval/EResult"),
              "ok",
              JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromInternalName(
                      JavaInternalName.of("compilerxyz/eval/EResult")),
                  TList.of(JvmConstants.DESC_OBJECT))));
      return EResult.ok(DynUtil.dynamicStackToValue(loc, new CodeDynamic(loc, okSeq), resultType));
    }
    var argTupleType =
        new Tuple(
            Opt.none(),
            TList.of(
                new TupleElement.Dynamic(new TypeStringDynamic()),
                new TupleElement.Dynamic(new TypeStringDynamic())));
    var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, argTupleType, argument));
    if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
    var slotsRes = DynUtil.destructurePositional(xx, loc, argTupleType, convertedOpt.get());
    if (slotsRes.hasErrors()) return slotsRes.reerr();
    var slots = slotsRes.requireValue("TypeEResultVariantConstructorDynamic::call destructure");
    if (slots.size() != 2) {
      throw new AssertionError(
          "TypeEResultVariantConstructorDynamic::call err: expected 2 slots, got " + slots.size());
    }
    var locJs = slots.get(0);
    var msgJs = slots.get(1);
    var errSeq = new JavaBytecodeSequence();
    errSeq.add(locJs);
    errSeq.add(msgJs);
    errSeq.add(
        JavaBytecodeUtils.callStaticMethod(
            loc,
            JavaInternalName.of("compilerxyz/eval/EResult"),
            "err",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromInternalName(
                    JavaInternalName.of("compilerxyz/eval/EResult")),
                TList.of(
                    JavaDataDescriptor.fromInternalName(
                        JavaInternalName.of("compilerxyz/eval/QExprId")),
                    JavaDataDescriptor.fromInternalName(
                        JavaInternalName.of("java/lang/String"))))));
    return EResult.ok(DynUtil.dynamicStackToValue(loc, new CodeDynamic(loc, errSeq), resultType));
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
