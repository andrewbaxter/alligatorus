package compilerxyz.eval.value.function;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.option.TypeOptionDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectTuple;

@BuiltinTypeId(7039L)
@BuiltinPath("eval.aspect_lookup")
public record TypeFunctionAspectDynamic()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  private static final JavaInternalName INTNAME_TYPE =
      JavaInternalName.fromClass(Type.class);
  private static final JavaMethodDescriptor DESC_TYPE_ASPECT =
      JavaMethodDescriptor.fromParts(
          JavaDataDescriptor.fromClass(compilerxyz.util.Opt.class),
          JavaDataDescriptor.fromClass(compilerxyz.eval.ExprContext.class),
          JavaDataDescriptor.fromClass(compilerxyz.eval.Unique.class));

  @Override
  public TypeId hasTypeIdTypeId() { return GenTypeIds.TYPE_FUNCTION_ASPECT_DYNAMIC_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "fn-aspect";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeFunctionAspectDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_FUNCTION_ASPECT_DYNAMIC_TYPE_ID, "function_aspect_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "aspect_lookup: arg has no compile-time value");
    }
    var argVal = argSome.value();
    var argType = argVal.valueType();
    var tupleOpt = compilerxyz.gen.GenAspectTuple.getMaybe(xx, argType);
    if (!(tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome)) {
      return EResult.err(loc, "aspect_lookup: expected 3-tuple arg (subject, aspect_type, xx)");
    }
    var elemCvs = tupleSome.value().aspectTupleTakeElements(xx, loc, argVal);
    if (elemCvs.size() != 3) {
      return EResult.err(
          loc,
          "aspect_lookup: expected 3 elements (subject, aspect_type, xx), got " + elemCvs.size());
    }
    if (elemCvs.get(0).hasErrors()) return elemCvs.get(0).reerr();
    if (elemCvs.get(1).hasErrors()) return elemCvs.get(1).reerr();
    if (elemCvs.get(2).hasErrors()) return elemCvs.get(2).reerr();
    var subjectCv = elemCvs.get(0).requireValue("aspect_lookup subject");
    var aspectTypeCv = elemCvs.get(1).requireValue("aspect_lookup aspect_type");
    var xxCv = elemCvs.get(2).requireValue("aspect_lookup xx");

    if (!(aspectTypeCv.value() instanceof Opt.Some<Value> atSome)) {
      return EResult.err(loc, "aspect_lookup: aspect_type has no compile-time value");
    }
    Object aspectTypeVal = atSome.value();
    if (aspectTypeVal instanceof compilerxyz.eval.value.object_.ValueObjectConst wrap) {
      aspectTypeVal = wrap.data();
    }
    if (!(aspectTypeVal instanceof compilerxyz.eval.TypeAspect aspTa)) {
      return EResult.err(
          loc,
          "aspect_lookup: aspect_type must implement TypeAspect, got " + aspectTypeVal.getClass());
    }
    var auq = aspTa.id();

    var anyDyn = compilerxyz.gen.GenType.dynamicType();
    var subjectConvertedRes =
        compilerxyz.eval.Eval.typecheckConvert(xx, loc, anyDyn, subjectCv);
    if (subjectConvertedRes.hasErrors()) return subjectConvertedRes.reerr();
    var subjectConverted =
        subjectConvertedRes.requireValue("aspect_lookup subject convert");
    var subjectConsumeRes =
        compilerxyz.gen.GenAspectConsumeDynamic.get(xx, loc, anyDyn);
    if (subjectConsumeRes.hasErrors()) return subjectConsumeRes.reerr();
    var subjectDynRes =
        subjectConsumeRes
            .requireValue("aspect_lookup subject consume")
            .aspectConsumeDynamicToDynamic(xx, loc, subjectConverted);
    if (subjectDynRes.hasErrors()) return subjectDynRes.reerr();
    var subjectDyn = subjectDynRes.requireValue("aspect_lookup subject toDynamic");
    var xxDyn = xxCv.code().dynamic();
    if (xxDyn == null) {
      return EResult.err(loc, "aspect_lookup: xx has no runtime code");
    }

    var uniqueKey = new JavaBytecodeBindingKey();
    var uniqueId = new BindingId.Local(uniqueKey);
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(uniqueKey, this, new External(Unique.class, auq)));
    var uniqueLoad = BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, uniqueId);

    var seq = new JavaBytecodeSequence();
    seq.add(subjectDyn);
    seq.add(xxDyn);
    seq.add(uniqueLoad);
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, INTNAME_TYPE, "typeAspect", DESC_TYPE_ASPECT));

    return EResult.ok(
        new CodeValue(
            Code.of(loc, new CodeDynamic(new CodeLoc.Some(loc), seq)),
            EvalOutcome.ofValue(
                compilerxyz.eval.value.ValueDynamic.stack(xx, new TypeOptionDynamic(compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, aspTa))))));
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
