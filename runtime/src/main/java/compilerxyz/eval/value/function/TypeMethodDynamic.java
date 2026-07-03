package compilerxyz.eval.value.function;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;

public record TypeMethodDynamic(
    Type receiver,
    Type arg,
    Type ret,
    JavaInternalName klass,
    String methodName,
    JavaMethodDescriptor descriptor,
    boolean needsExprContext)
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  private static final JavaDataDescriptor DESC_EXPR_CONTEXT =
      JavaDataDescriptor.fromInternalName(JavaInternalName.of("compilerxyz/eval/ExprContext"));

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> drop(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "dynamic method";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.FunctionDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var receiverDyn = self.code().dynamic();
    if (receiverDyn == null) {
      return EResult.err(loc, "TypeMethodDynamic::call: receiver has no dynamic code");
    }
    var argConsumeRes = AspectConsumeDynamic.get(xx, loc, arg);
    if (argConsumeRes.hasErrors()) return argConsumeRes.reerr();
    var convertedRes = Eval.typecheckConvert(xx, loc, arg, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var argDynRes = argConsumeRes.valueOrNull().toDynamic(xx, loc, convertedRes.valueOrNull());
    if (argDynRes.hasErrors()) return argDynRes.reerr();
    var retStackRes = AspectStackValueDynamic.get(xx, loc, ret);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var argList = new compilerxyz.util.TList<CodeDynamic>();
    argList.add(receiverDyn);
    // TypeNullDynamic is a language-level "no arg" marker — it has no JVM stack
    // representation in the method descriptor, so don't push it.
    if (!(arg instanceof compilerxyz.eval.value.null_.TypeNullDynamic)) {
      argList.add(argDynRes.valueOrNull());
    }
    return EResult.ok(
        retStackRes.valueOrNull().toValue(loc, emitInvocation(loc, xx.vx, argList)));
  }

  public CodeDynamic emitInvocation(
      QExprId loc, compilerxyz.eval.EvalContext vx, compilerxyz.util.TList<CodeDynamic> args) {
    var seq = new JavaBytecodeSequence();
    if (needsExprContext) {
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc,
              JavaInternalName.of(vx.evalRootInternalName),
              compilerxyz.eval.dynamic.EvalRootEmitter.EXPR_CONTEXT_FIELD,
              compilerxyz.eval.dynamic.EvalRootEmitter.DESC_EXPR_CONTEXT));
    }
    // Each language arg's Type owns its stack JVM class via AspectConsumeDynamic,
    // and the code produced by that aspect already leaves a properly-typed value
    // on the stack. No CHECKCAST here.
    for (var argCode : args) {
      seq.add(argCode);
    }
    seq.add(JavaBytecodeUtils.callStaticMethod(loc, klass, methodName, descriptor));
    return new CodeDynamic(loc, seq);
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeMethodDynamic::toDynamic: CodeValue has no dynamic code");
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
