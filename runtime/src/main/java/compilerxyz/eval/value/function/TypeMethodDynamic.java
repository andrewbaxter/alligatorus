package compilerxyz.eval.value.function;

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

@BuiltinLight(uniqueId = 6112L)
public record TypeMethodDynamic(
    Type receiver,
    TList<FunctionParam> params,
    Type ret,
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
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
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
    var receiverDyn = self.code().dynamic();
    if (receiverDyn == null) {
      return EResult.err(loc, "TypeMethodDynamic::call: receiver has no dynamic code");
    }
    var argType = arg();
    compilerxyz.eval.EResult<compilerxyz.util.TList<CodeDynamic>> argsDynRes;
    if (compilerxyz.eval.dynamic.DynUtil.zeroArgSignature(xx, argType)) {
      argsDynRes = compilerxyz.eval.EResult.ok(new compilerxyz.util.TList<>());
    } else {
      var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
      if (convertedRes.hasErrors()) return convertedRes.reerr();
      var converted = convertedRes.requireValue("TypeMethodDynamic::call converted arg");
      argsDynRes =
          compilerxyz.eval.dynamic.DynUtil.destructurePositional(xx, loc, argType, converted);
      if (argsDynRes.hasErrors()) return argsDynRes.reerr();
    }
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, ret);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var seq = new JavaBytecodeSequence();
    seq.add(receiverDyn);
    if (needsExprContext) {
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc,
              JavaInternalName.of(xx.vx.evalRootInternalName),
              compilerxyz.eval.dynamic.EvalRootEmitter.EXPR_CONTEXT_FIELD,
              compilerxyz.eval.dynamic.EvalRootEmitter.DESC_EXPR_CONTEXT));
    }
    var args = argsDynRes.requireValue("TypeMethodDynamic::call args");
    var paramDescs = descriptor.parameterDescriptors();
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
    return EResult.ok(
        retStackRes
            .requireValue("TypeMethodDynamic::call ret stack")
            .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
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
    var dyn = cv.code().dynamic();
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_METHOD_DYNAMIC_IDENTITY, TypeMethodDynamic.class) instanceof Opt.Some<TypeMethodDynamic> oSome
        && receiver.typeIsEqual(xx, oSome.value().receiver) && TypeFunctionDynamic.paramsEqual(xx, params, oSome.value().params) && ret.typeIsEqual(xx, oSome.value().ret) && klass.equals(oSome.value().klass) && methodName.equals(oSome.value().methodName) && isInterface == oSome.value().isInterface && needsExprContext == oSome.value().needsExprContext;
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
