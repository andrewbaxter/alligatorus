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
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

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
        AspectDropDynamicLight,
        AspectFunction,
        AspectInitialBind,
        compilerxyz.gen.AspectLift,
        ExporterAny {

  @Override
  public EResult<Value> lift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    // The externalHostValue can be either an actual TypeFunctionDynamic (rare) or the JVM
    // receiver value for a method-value access. Either way, the compile-time type is the
    // TypeFunctionDynamic itself, so wrap `this` as the const value.
    return EResult.ok(new compilerxyz.eval.value.any.ValueAnyConst(this));
  }

  @Override
  public TypeId typeId() { return new TypeId.Composite(GenTypeIds.TYPE_FUNCTION_DYNAMIC_TYPE_ID, TList.of(arg.typeId(), ret.typeId())); }


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
    if (ret instanceof compilerxyz.gen.AspectConsumeDynamic retConsume) {
      var retDesc = retConsume.consumeDynamicJvmClass();
      var d = retDesc.value();
      if (!d.equals(compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT.value())
          && d.charAt(0) == 'L') {
        seq.add(JavaBytecodeUtils.cast(loc, retDesc));
      }
    }
    return new CodeDynamic(loc, seq);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
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
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(compilerxyz.gen.AspectLift.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var convertedRes = Eval.typecheckConvert(xx, loc, arg, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeFunctionDynamic::call converted arg");
    var retStackRes = AspectStackValueDynamic.get(xx, loc, ret);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var argList = new compilerxyz.util.TList<CodeDynamic>();
    var selfDyn = self.code().dynamic();
    if (selfDyn != null) argList.add(selfDyn);
    var contextSlots = needsExprContext ? 1 : 0;
    var receiverSlots = selfDyn != null ? 1 : 0;
    var remainingSlots =
        parameterCount(descriptor.value()) - contextSlots - receiverSlots;
    if (remainingSlots == 1) {
      var argsDynRes = destructurePackedObject(xx, loc, arg, converted);
      if (argsDynRes.hasErrors()) return argsDynRes.reerr();
      argList.add(argsDynRes.requireValue("TypeFunctionDynamic::call packed arg"));
    } else {
      var argsDynRes = destructurePositional(xx, loc, arg, converted);
      if (argsDynRes.hasErrors()) return argsDynRes.reerr();
      for (var c : argsDynRes.requireValue("TypeFunctionDynamic::call positional args")) {
        argList.add(c);
      }
    }
    return EResult.ok(
        retStackRes
            .requireValue("TypeFunctionDynamic::call ret stack")
            .toValue(loc, emitInvocation(loc, xx.vx, argList)));
  }

  private static EResult<TList<CodeDynamic>> destructurePositional(
      ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
    var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, argType);
    if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
      if (!(converted.value() instanceof Opt.Some<Value> some)) {
        return EResult.err(loc, "TypeFunctionDynamic::destructure: arg has no compile-time value");
      }
      var elementTypes = tupleSome.value().elementTypes();
      var elemCvs = tupleSome.value().takeElements(xx, loc, some.value());
      if (elementTypes.size() != elemCvs.size()) {
        throw new IllegalStateException(
            "TypeFunctionDynamic::destructure: elementTypes/takeElements size mismatch");
      }
      var out = new TList<CodeDynamic>(elementTypes.size());
      for (int i = 0; i < elementTypes.size(); i++) {
        var elemCvRes = elemCvs.get(i);
        if (elemCvRes.hasErrors()) return elemCvRes.reerr();
        var elemCv = elemCvRes.requireValue("TypeFunctionDynamic::destructure element");
        var consumeRes = AspectConsumeDynamic.get(xx, loc, elementTypes.get(i));
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var elemDyn =
            consumeRes
                .requireValue("TypeFunctionDynamic::destructure consume")
                .toDynamic(xx, loc, elemCv);
        if (elemDyn.hasErrors()) return elemDyn.reerr();
        out.add(elemDyn.requireValue("TypeFunctionDynamic::destructure elem toDynamic"));
      }
      return EResult.ok(out);
    }
    var consumeRes = AspectConsumeDynamic.get(xx, loc, argType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var argDyn =
        consumeRes
            .requireValue("TypeFunctionDynamic::call arg consume")
            .toDynamic(xx, loc, converted);
    if (argDyn.hasErrors()) return argDyn.reerr();
    return EResult.ok(TList.of(argDyn.requireValue("TypeFunctionDynamic::call arg")));
  }

  private static int parameterCount(String descriptor) {
    int n = 0;
    int i = 1;
    while (i < descriptor.length() && descriptor.charAt(i) != ')') {
      var c = descriptor.charAt(i);
      if (c == 'L') {
        n++;
        i = descriptor.indexOf(';', i) + 1;
      } else if (c == '[') {
        i++;
      } else {
        n++;
        i++;
      }
    }
    return n;
  }

  private static EResult<CodeDynamic> destructurePackedObject(
      ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
    var positional = destructurePositional(xx, loc, argType, converted);
    if (positional.hasErrors()) return positional.reerr();
    var elemCodes = positional.requireValue("TypeFunctionDynamic::destructure packed elements");
    if (elemCodes.isEmpty()) {
      return EResult.ok(JavaBytecodeUtils.literalNull(loc));
    }
    if (elemCodes.size() == 1) {
      return EResult.ok(elemCodes.get(0));
    }
    return EResult.ok(JavaBytecodeUtils.buildObjectArray(loc, elemCodes));
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
    return new BindingFunctionDynamic(dynamicKey, this);
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
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
