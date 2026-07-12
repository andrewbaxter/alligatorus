package compilerxyz.eval.value.function;

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
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.TList;

public record TypeMethodDynamic(
    Type receiver,
    Type arg,
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
  public TypeId typeId() { return new TypeId.Composite(GenTypeIds.TYPE_METHOD_DYNAMIC_TYPE_ID, TList.of(receiver.typeId(), arg.typeId(), ret.typeId())); }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "dynamic method";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.FunctionDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)) {
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
    var convertedRes = Eval.typecheckConvert(xx, loc, arg, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeMethodDynamic::call converted arg");
    var argsDynRes = compilerxyz.eval.dynamic.DynUtil.destructurePositional(xx, loc, arg, converted);
    if (argsDynRes.hasErrors()) return argsDynRes.reerr();
    var retStackRes = AspectStackValueDynamic.get(xx, loc, ret);
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
  public Opt<Type> aspectFunctionExceptionType() {
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
    return EResult.ok(this);
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
