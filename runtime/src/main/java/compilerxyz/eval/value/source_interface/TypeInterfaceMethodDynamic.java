package compilerxyz.eval.value.source_interface;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeInterfaceMethodDynamic(InterfaceData iface, InterfaceMethodData method)
    implements Type, AspectConsumeDynamic, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public TypeId typeId() {
    var argType = method.argType;
    var retType = method.retType;
    if (argType == null || retType == null) {
      throw new IllegalStateException(
          "TypeInterfaceMethodDynamic.typeId: method '"
              + method.name
              + "' missing arg/ret type");
    }
    return new TypeId.Composite(
        GenTypeIds.TYPE_INTERFACE_METHOD_DYNAMIC_TYPE_ID,
        TList.of(argType.typeId(), retType.typeId()));
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeInterfaceMethodDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public String typeDesc() {
    return "interface method " + method.name;
  }

  @Override
  public Type valueType() {
    return new TypeRoot.FunctionDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var argType = method.argType;
    var retType = method.retType;
    if (argType == null || retType == null) {
      return EResult.err(
          loc,
          "TypeInterfaceMethodDynamic::call: method '"
              + method.name
              + "' missing arg or ret type");
    }
    var ifaceInternalName = JavaInternalName.of(iface.compiledBinaryName.replace('.', '/'));

    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeInterfaceMethodDynamic::call: self has no dynamic code");
    }
    var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeInterfaceMethodDynamic typecheckConvert");
    var consumeRes = AspectConsumeDynamic.get(xx, loc, argType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var argDynRes =
        consumeRes
            .requireValue("TypeInterfaceMethodDynamic AspectConsumeDynamic.get")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (argDynRes.hasErrors()) return argDynRes.reerr();
    var argDyn = argDynRes.requireValue("TypeInterfaceMethodDynamic argDyn");

    var retStackRes = AspectStackValueDynamic.get(xx, loc, retType);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var descObject = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    var methodDesc = JavaMethodDescriptor.fromParts(descObject, TList.of(descObject));

    var seq = new JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(ifaceInternalName)));
    seq.add(argDyn);
    var hostName = method.hostMethodName == null ? method.name : method.hostMethodName;
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, ifaceInternalName, hostName, methodDesc));
    var retConsumeOpt = AspectConsumeDynamic.getMaybe(xx, retType);
    if (retConsumeOpt instanceof Opt.Some<AspectConsumeDynamic> retConsumeSome) {
      var retDesc = retConsumeSome.value().aspectConsumeDynamicJvmClass(xx);
      if (!retDesc.value().equals(descObject.value())) {
        var c = retDesc.value().charAt(0);
        if (c == 'L' || c == '[') {
          seq.add(JavaBytecodeUtils.cast(loc, retDesc));
        }
      }
    }
    return EResult.ok(
        retStackRes
            .requireValue("TypeInterfaceMethodDynamic AspectStackValueDynamic.get")
            .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
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
    return EResult.ok(this);
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
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
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
