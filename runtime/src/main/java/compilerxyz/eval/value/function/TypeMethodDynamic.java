package compilerxyz.eval.value.function;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.dynamic.JvmConstants;

@BuiltinLight(uniqueId = 6112L)
public record TypeMethodDynamic(
    Type receiver,
    TList<FunctionParam> params,
    Opt<Type> ret,
    JavaInternalName klass,
    String methodName,
    JavaMethodDescriptor descriptor,
    boolean isInterface,
    boolean needsExprContext)
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public String toString() {
    return "TypeMethodDynamic[" + klass.value + "." + methodName + descriptor.value() + "]";
  }

  public Type arg() {
    return FunctionParam.argType(params);
  }



  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(
        JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "dynamic method";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_METHOD_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var receiverDyn = self.code().codeDynamic();
    if (receiverDyn == null) {
      return EResult.err(loc, "TypeMethodDynamic::call: receiver has no dynamic code");
    }
    var argType = arg();
    EResult<TList<CodeDynamic>> argsDynRes;
    if (DynUtil.zeroArgSignature(xx, argType)) {
      argsDynRes = EResult.ok(new TList<>());
    } else {
      var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
      if (convertedRes.hasErrors()) return convertedRes.reerr();
      var converted = convertedRes.requireValue("TypeMethodDynamic::call converted arg");
      argsDynRes =
          DynUtil.destructurePositional(xx, loc, argType, converted);
      if (argsDynRes.hasErrors()) return argsDynRes.reerr();
    }
    var seq = new JavaBytecodeSequence();
    seq.add(receiverDyn);
    if (needsExprContext) {
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc,
              JavaInternalName.of(xx.vx.evalRootInternalName),
              EvalRootEmitter.EXPR_CONTEXT_FIELD,
              EvalRootEmitter.DESC_EXPR_CONTEXT));
    }
    var args = argsDynRes.requireValue("TypeMethodDynamic::call args");
    var paramDescs = new TList<JavaDataDescriptor>();
    {
      var _pd = descriptor.value();
      if (_pd.isEmpty() || _pd.charAt(0) != '(') {
        throw new IllegalStateException("JavaMethodDescriptor missing '(': " + _pd);
      }
      int _pdi = 1;
      while (_pdi < _pd.length() && _pd.charAt(_pdi) != ')') {
        int _pdStart = _pdi;
        while (_pdi < _pd.length() && _pd.charAt(_pdi) == '[') _pdi++;
        char _pdc = _pd.charAt(_pdi);
        if (_pdc == 'L') {
          while (_pdi < _pd.length() && _pd.charAt(_pdi) != ';') _pdi++;
          _pdi++;
        } else {
          _pdi++;
        }
        paramDescs.add(JavaDataDescriptor.create(_pd.substring(_pdStart, _pdi)));
      }
    }
    var paramOffset = needsExprContext ? 1 : 0;
    for (int i = 0; i < args.size(); i++) {
      seq.add(args.get(i));
      var pi = i + paramOffset;
      if (pi < paramDescs.size()) {
        var expected = paramDescs.get(pi);
        if (expected.value().startsWith("L") && !expected.value().equals("Ljava/lang/Object;")) {
          seq.add(JavaBytecodeUtils.cast(loc, expected));
        }
      }
    }
    if (isInterface) {
      seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, klass, methodName, descriptor));
    } else {
      seq.add(JavaBytecodeUtils.callMethod(loc, klass, methodName, descriptor));
    }
    var seqCode = new CodeDynamic(loc, seq);
    if (ret.optIsNone()) {
      return EResult.ok(new CodeValue(Code.of(loc, seqCode), EvalOutcome.NO_VALUE));
    }
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, ret.optUnwrap());
    if (retStackRes.hasErrors()) return retStackRes.reerr();
    return EResult.ok(
        retStackRes
            .requireValue("TypeMethodDynamic::call ret stack")
            .aspectStackValueDynamicToValue(xx, loc, seqCode));
  }


  @Override
  public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeMethodDynamic::toDynamic: CodeValue has no dynamic code");
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
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_METHOD_DYNAMIC_IDENTITY, TypeMethodDynamic.class);
    return oOpt.optIsSome() && receiver.typeIsEqual(xx, oOpt.optUnwrap().receiver) && TypeFunctionDynamic.paramsEqual(xx, params, oOpt.optUnwrap().params) && TypeFunctionDynamic.retEqual(xx, ret, oOpt.optUnwrap().ret) && klass.equals(oOpt.optUnwrap().klass) && methodName.equals(oOpt.optUnwrap().methodName) && isInterface == oOpt.optUnwrap().isInterface && needsExprContext == oOpt.optUnwrap().needsExprContext;
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
