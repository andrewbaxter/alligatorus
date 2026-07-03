package compilerxyz.eval.value.function;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
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
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeFunctionDynamic(
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
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  private static final JavaDataDescriptor DESC_EXPR_CONTEXT =
      JavaDataDescriptor.fromInternalName(JavaInternalName.of("compilerxyz/eval/ExprContext"));
  private static final JavaDataDescriptor DESC_OBJECT =
      JavaDataDescriptor.fromClass(Object.class);
  private static final JavaMethodDescriptor DESC_CTX_OBJ_TO_OBJ =
      JavaMethodDescriptor.fromParts(DESC_OBJECT, TList.of(DESC_EXPR_CONTEXT, DESC_OBJECT));

  /**
   * Convenience factory for builtin function types whose Java implementation is a static method
   * {@code Object <klassName>.<methodName>(ExprContext, Object)}.
   */
  public static TypeFunctionDynamic builtinWithCtx(
      String klassName, String methodName, Type argType, Type retType) {
    return new TypeFunctionDynamic(
        argType, retType, JavaInternalName.of(klassName), methodName, DESC_CTX_OBJ_TO_OBJ, true);
  }

  /**
   * Low-level: emit the INVOKESTATIC (with an optional preceding GETSTATIC for {@code
   * __exprContext}) using pre-built {@link CodeDynamic} args. Bypasses source-level typecheck and
   * dynamic-lowering because the internal callers (e.g. invisible-builtin dispatch) already have
   * their args in {@code CodeDynamic} form.
   */
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
    // The arg's stack JVM class is declared via AspectConsumeDynamic and the code
    // it produces already leaves a properly-typed value. No CHECKCAST here.
    for (var argCode : args) {
      seq.add(argCode);
    }
    seq.add(JavaBytecodeUtils.callStaticMethod(loc, klass, methodName, descriptor));
    return new CodeDynamic(loc, seq);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaInternalName consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT;
  }

  @Override
  public String typeDesc() {
    return "dynamic func";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.FunctionDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var argConsumeRes = AspectConsumeDynamic.get(xx, loc, arg);
    if (argConsumeRes.hasErrors()) return argConsumeRes.reerr();
    var convertedRes = Eval.typecheckConvert(xx, loc, arg, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var argDynRes = argConsumeRes.valueOrNull().toDynamic(xx, loc, convertedRes.valueOrNull());
    if (argDynRes.hasErrors()) return argDynRes.reerr();
    var retStackRes = AspectStackValueDynamic.get(xx, loc, ret);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var argList = new compilerxyz.util.TList<CodeDynamic>();
    argList.add(argDynRes.valueOrNull());
    return EResult.ok(
        retStackRes.valueOrNull().toValue(loc, emitInvocation(loc, xx.vx, argList)));
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
        loc, "TypeFunctionDynamic::toDynamic: CodeValue has no dynamic code");
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
