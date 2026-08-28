package compilerxyz.eval.value.function;

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
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
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
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.eval.TypeAspect;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.Eval;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.gen.GenType;
import compilerxyz.artifact.Artifact;

@BuiltinPath("eval.aspect_lookup")
@BuiltinLight(uniqueId = 6108L)
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
          JavaDataDescriptor.fromClass(ExprContext.class),
          JavaDataDescriptor.fromClass(compilerxyz.eval.Unique.class));


  @Override
  public String typeDesc() {
    return "fn-aspect";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_FUNCTION_ASPECT_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (argument.value().optIsNone()) {
      return EResult.err(loc, "aspect_lookup: arg has no compile-time value");
    }
    var argVal = argument.value().optUnwrap();
    var argType = argVal.valueType();
    var tupleOpt = GenAspectTuple.getMaybe(xx, argType);
    if (tupleOpt.optIsNone()) {
      return EResult.err(loc, "aspect_lookup: expected 3-tuple arg (subject, aspect_type, xx)");
    }
    var elemCvs = tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argVal);
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

    if (aspectTypeCv.value().optIsNone()) {
      return EResult.err(loc, "aspect_lookup: aspect_type has no compile-time value");
    }
    Object aspectTypeVal = aspectTypeCv.value().optUnwrap();
    if (aspectTypeVal instanceof ValueObjectConst wrap) {
      aspectTypeVal = wrap.data();
    }
    if (!(aspectTypeVal instanceof TypeAspect aspTa)) {
      return EResult.err(
          loc,
          "aspect_lookup: aspect_type must implement TypeAspect, got " + aspectTypeVal.getClass());
    }
    if (aspectTypeVal
            instanceof TypeSourceInterfaceDynamic si
        && !si.data().isAspect) {
      return EResult.err(
          loc,
          "aspect_lookup: interface is not an aspect (set_is_aspect was not called)");
    }
    var auq = aspTa.typeAspectId();

    var anyDyn = GenType.dynamicType();
    var subjectConvertedRes =
        Eval.typecheckConvert(xx, loc, anyDyn, subjectCv);
    if (subjectConvertedRes.hasErrors()) return subjectConvertedRes.reerr();
    var subjectConverted =
        subjectConvertedRes.requireValue("aspect_lookup subject convert");
    var subjectConsumeRes =
        GenAspectConsumeDynamic.get(xx, loc, anyDyn);
    if (subjectConsumeRes.hasErrors()) return subjectConsumeRes.reerr();
    var subjectDynRes =
        subjectConsumeRes
            .requireValue("aspect_lookup subject consume")
            .aspectConsumeDynamicToDynamic(xx, loc, subjectConverted);
    if (subjectDynRes.hasErrors()) return subjectDynRes.reerr();
    var subjectDyn = subjectDynRes.requireValue("aspect_lookup subject toDynamic");
    var xxDyn = xxCv.code().codeDynamic();
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
                ValueDynamic.stack(new TypeOptionDynamic(DynUtil.substituteBoxed(xx, aspTa)), 1))));
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
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_FUNCTION_ASPECT_DYNAMIC_IDENTITY).optIsSome();
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

  @Override
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return Type.exportArtifact(xx, loc, this);
  }
}
