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
        compilerxyz.gen.AspectElementUseDynamicBind,
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
    if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
      for (var elem : tupleSome.value().aspectTupleElementTypes()) {
        var elemDescRes = jvmDescOf(xx, loc, elem);
        if (elemDescRes.hasErrors()) return elemDescRes.reerr();
        argDescs.add(elemDescRes.requireValue("TypeFunctionDynamic::descriptor tuple elem"));
      }
    } else {
      var recordOpt = compilerxyz.gen.GenAspectRecord.getMaybe(xx, arg);
      if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
        for (var entry : recordSome.value().aspectRecordFieldTypes().entrySet()) {
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
    if (ret instanceof Opt.Some<Type> retSome) {
      var retDescRes = jvmDescOf(xx, loc, retSome.value());
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
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
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
    if (!(ret instanceof Opt.Some<Type> retSome)) {
      return EResult.ok(new CodeValue(Code.of(loc, invocation), EvalOutcome.NO_VALUE));
    }
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retSome.value());
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
    var dyn = cv.code().dynamic();
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
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_FUNCTION_DYNAMIC_IDENTITY, TypeFunctionDynamic.class) instanceof Opt.Some<TypeFunctionDynamic> oSome
        && paramsEqual(xx, params, oSome.value().params) && retEqual(xx, ret, oSome.value().ret) && klass.equals(oSome.value().klass) && methodName.equals(oSome.value().methodName) && needsExprContext == oSome.value().needsExprContext;
  }

  static boolean paramsEqual(
      compilerxyz.eval.ExprContext xx, TList<FunctionParam> a, TList<FunctionParam> b) {
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      if (!a.get(i).name().equals(b.get(i).name())) return false;
      if (!a.get(i).type().typeIsEqual(xx, b.get(i).type())) return false;
    }
    return true;
  }

  public static boolean retEqual(compilerxyz.eval.ExprContext xx, Opt<Type> a, Opt<Type> b) {
    if (a instanceof Opt.Some<Type> as && b instanceof Opt.Some<Type> bs) {
      return as.value().typeIsEqual(xx, bs.value());
    }
    return a instanceof Opt.None && b instanceof Opt.None;
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

  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return new compilerxyz.eval.value.function.BindingFunctionDynamic(dynamicKey, this);
  }
}
