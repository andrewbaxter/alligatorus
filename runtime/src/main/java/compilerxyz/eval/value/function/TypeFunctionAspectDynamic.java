package compilerxyz.eval.value.function;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
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
import compilerxyz.eval.Value;
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
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.option.TypeOptionDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeFunctionAspectDynamic()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  private static final JavaInternalName INTNAME_TYPE =
      JavaInternalName.of("compilerxyz/eval/Type");
  private static final JavaMethodDescriptor DESC_TYPE_ASPECT =
      JavaMethodDescriptor.fromParts(
          JavaDataDescriptor.fromClass(compilerxyz.util.Opt.class),
          JavaDataDescriptor.fromClass(compilerxyz.eval.ExprContext.class),
          JavaDataDescriptor.fromClass(compilerxyz.eval.Unique.class));

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_FUNCTION_ASPECT_DYNAMIC_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "fn-aspect";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.FunctionAspectDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID) || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "aspect_lookup: arg has no compile-time value");
    }
    var argVal = argSome.value();
    var argType = argVal.valueTypeOf();
    var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, argType);
    if (!(tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome)) {
      return EResult.err(loc, "aspect_lookup: expected 3-tuple arg (subject, aspect_type, xx)");
    }
    var elemCvs = tupleSome.value().takeElements(xx, loc, argVal);
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
    var aspectTypeVal = atSome.value();
    if (aspectTypeVal instanceof compilerxyz.eval.value.object_.ValueObjectConst wrap) {
      aspectTypeVal = wrap.inner();
    }
    if (!(aspectTypeVal instanceof compilerxyz.eval.TypeAspect aspTa)) {
      return EResult.err(
          loc,
          "aspect_lookup: aspect_type must implement TypeAspect, got " + aspectTypeVal.getClass());
    }
    var auq = aspTa.id();

    var anyDyn = new compilerxyz.eval.value.any.TypeAnyDynamic();
    var subjectConvertedRes =
        compilerxyz.eval.Eval.typecheckConvert(xx, loc, anyDyn, subjectCv);
    if (subjectConvertedRes.hasErrors()) return subjectConvertedRes.reerr();
    var subjectConverted =
        subjectConvertedRes.requireValue("aspect_lookup subject convert");
    var subjectConsumeRes =
        compilerxyz.gen.AspectConsumeDynamic.get(xx, loc, anyDyn);
    if (subjectConsumeRes.hasErrors()) return subjectConsumeRes.reerr();
    var subjectDynRes =
        subjectConsumeRes
            .requireValue("aspect_lookup subject consume")
            .toDynamic(xx, loc, subjectConverted);
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
            Opt.some(
                compilerxyz.eval.value.ValueDynamic.stack(new TypeOptionDynamic(aspTa)))));
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
