package compilerxyz.eval.value.function;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EvalOutcome;
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
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
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
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectRecord;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.eval.value.function.BindingFunctionDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectRecord;
import compilerxyz.gen.AspectTuple;

@BuiltinLight(uniqueId = 6110L)
public record TypeFunctionDynamic(
    TList<FunctionParam> params,
    Opt<Type> ret,
    JavaInternalName klass,
    String methodName,
    boolean needsExprContext)
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectFunction,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        ExporterAny {


  public Type arg() {
    return FunctionParam.argType(params);
  }

  public EResult<JavaMethodDescriptor> descriptor(ExprContext xx, QExprId loc) {
    return descriptorFor(xx, loc, arg(), ret, needsExprContext);
  }

  public static EResult<JavaMethodDescriptor> descriptorFor(
      ExprContext xx, QExprId loc, Type arg, Opt<Type> ret, boolean needsExprContext) {
    var argDescs = new TList<JavaDataDescriptor>();
    if (needsExprContext) argDescs.add(JavaDataDescriptor.fromClass(ExprContext.class));
    var tupleOpt = compilerxyz.gen.GenAspectTuple.getMaybe(xx, arg);
    if (tupleOpt.optIsSome()) {
      for (var elem : tupleOpt.optUnwrap().aspectTupleElementTypes()) {
        var elemDescRes = jvmDescOf(xx, loc, elem);
        if (elemDescRes.hasErrors()) return elemDescRes.reerr();
        argDescs.add(elemDescRes.requireValue("TypeFunctionDynamic::descriptor tuple elem"));
      }
    } else {
      var recordOpt = compilerxyz.gen.GenAspectRecord.getMaybe(xx, arg);
      if (recordOpt.optIsSome()) {
        for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
          var fieldDescRes = jvmDescOf(xx, loc, entry.getValue());
          if (fieldDescRes.hasErrors()) return fieldDescRes.reerr();
          argDescs.add(fieldDescRes.requireValue("TypeFunctionDynamic::descriptor record field"));
        }
      } else {
        var argDescRes = jvmDescOf(xx, loc, arg);
        if (argDescRes.hasErrors()) return argDescRes.reerr();
        argDescs.add(argDescRes.requireValue("TypeFunctionDynamic::descriptor single arg"));
      }
    }
    JavaDataDescriptor retDesc;
    if (ret.optIsSome()) {
      var retDescRes = jvmDescOf(xx, loc, ret.optUnwrap());
      if (retDescRes.hasErrors()) return retDescRes.reerr();
      retDesc = retDescRes.requireValue("TypeFunctionDynamic::descriptor ret");
    } else {
      retDesc = JavaDataDescriptor.DESC_VOID;
    }
    return EResult.ok(JavaMethodDescriptor.fromParts(retDesc, argDescs));
  }

  public static EResult<CodeDynamic> emitStaticInvocation(
      ExprContext xx,
      QExprId loc,
      JavaInternalName klass,
      String methodName,
      JavaMethodDescriptor desc,
      boolean needsExprContext,
      TList<CodeDynamic> args) {
    var seq = new JavaBytecodeSequence();
    if (needsExprContext) {
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc,
              JavaInternalName.of(xx.vx.evalRootInternalName),
              compilerxyz.eval.dynamic.EvalRootEmitter.EXPR_CONTEXT_FIELD,
              compilerxyz.eval.dynamic.EvalRootEmitter.DESC_EXPR_CONTEXT));
    }
    for (var argCode : args) {
      seq.add(argCode);
    }
    seq.add(JavaBytecodeUtils.callStaticMethod(loc, klass, methodName, desc));
    return EResult.ok(new CodeDynamic(loc, seq));
  }

  private static EResult<JavaDataDescriptor> jvmDescOf(ExprContext xx, QExprId loc, Type t) {
    var consumeRes = GenAspectConsumeDynamic.get(xx, loc, t);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    return EResult.ok(
        consumeRes
            .requireValue("TypeFunctionDynamic::jvmDescOf consume")
            .aspectConsumeDynamicJvmClass(xx));
  }

  public EResult<CodeDynamic> emitInvocation(
      ExprContext xx,
      QExprId loc,
      compilerxyz.util.TList<CodeDynamic> args) {
    var descRes = descriptor(xx, loc);
    if (descRes.hasErrors()) return descRes.reerr();
    return emitStaticInvocation(
        xx,
        loc,
        klass,
        methodName,
        descRes.requireValue("TypeFunctionDynamic::emit desc"),
        needsExprContext,
        args);
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "dynamic func";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_FUNCTION_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(compilerxyz.gen.GenAspectElementUseDynamicBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var argType = arg();
    var argList = new compilerxyz.util.TList<CodeDynamic>();
    compilerxyz.eval.EResult<compilerxyz.util.TList<CodeDynamic>> argsDynRes;
    if (compilerxyz.eval.dynamic.DynUtil.zeroArgSignature(xx, argType)) {
      argsDynRes = compilerxyz.eval.EResult.ok(new compilerxyz.util.TList<>());
    } else {
      var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
      if (convertedRes.hasErrors()) return convertedRes.reerr();
      var converted = convertedRes.requireValue("TypeFunctionDynamic::call converted arg");
      argsDynRes =
          compilerxyz.eval.dynamic.DynUtil.destructurePositional(xx, loc, argType, converted);
      if (argsDynRes.hasErrors()) return argsDynRes.reerr();
    }
    for (var c : argsDynRes.requireValue("TypeFunctionDynamic::call positional args")) {
      argList.add(c);
    }
    var invocationRes = emitInvocation(xx, loc, argList);
    if (invocationRes.hasErrors()) return invocationRes.reerr();
    var invocation = invocationRes.requireValue("TypeFunctionDynamic::call invocation");
    if (ret.optIsNone()) {
      return EResult.ok(new CodeValue(Code.of(loc, invocation), EvalOutcome.NO_VALUE));
    }
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, ret.optUnwrap());
    if (retStackRes.hasErrors()) return retStackRes.reerr();
    return EResult.ok(
        retStackRes
            .requireValue("TypeFunctionDynamic::call ret stack")
            .aspectStackValueDynamicToValue(xx, loc, invocation));
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
    return EResult.err(
        loc, "TypeFunctionDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingFunctionDynamic(dynamicKey, this);
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
  public compilerxyz.eval.EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_FUNCTION_DYNAMIC_IDENTITY, TypeFunctionDynamic.class);
    return oOpt.optIsSome()
        && paramsEqual(xx, params, oOpt.optUnwrap().params) && retEqual(xx, ret, oOpt.optUnwrap().ret) && klass.equals(oOpt.optUnwrap().klass) && methodName.equals(oOpt.optUnwrap().methodName) && needsExprContext == oOpt.optUnwrap().needsExprContext;
  }

  static boolean paramsEqual(
      ExprContext xx, TList<FunctionParam> a, TList<FunctionParam> b) {
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      if (!a.get(i).name().equals(b.get(i).name())) return false;
      if (!a.get(i).type().typeIsEqual(xx, b.get(i).type())) return false;
    }
    return true;
  }

  public static boolean retEqual(ExprContext xx, Opt<Type> a, Opt<Type> b) {
    if (a.optIsSome() && b.optIsSome()) {
      return a.optUnwrap().typeIsEqual(xx, b.optUnwrap());
    }
    return a.optIsNone() && b.optIsNone();
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
  public compilerxyz.eval.EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingFunctionDynamic(dynamicKey, this);
  }
}
