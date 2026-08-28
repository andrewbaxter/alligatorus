package compilerxyz.eval.value.source_interface;

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
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.artifact.Artifact;

@BuiltinLight(uniqueId = 6136L)
public record TypeInterfaceMethodDynamic(InterfaceData iface, InterfaceMethodData method)
    implements Type, AspectConsumeDynamic, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeInterfaceMethodDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public String typeDesc() {
    return "interface method " + method.name;
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_INTERFACE_METHOD_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var argType = method.argType;
    var retType = method.retType;
    var ifaceInternalName = JavaInternalName.of(iface.compiledBinaryName.replace('.', '/'));

    var splitRes =
        DynUtil.methodRefSplitArgument(
            xx, loc, argType, argument, "interface method '" + method.name + "'");
    if (splitRes.hasErrors()) return splitRes.reerr();
    var split = splitRes.requireValue("TypeInterfaceMethodDynamic split");
    var ifaceType = new TypeSourceInterfaceDynamic(iface);
    var receiverRes = Eval.typecheckConvert(xx, loc, ifaceType, split.receiver());
    if (receiverRes.hasErrors()) return receiverRes.reerr();
    var receiverDynRes =
        ifaceType.aspectConsumeDynamicToDynamic(
            xx, loc, receiverRes.requireValue("TypeInterfaceMethodDynamic receiver"));
    if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
    var selfDyn = receiverDynRes.requireValue("TypeInterfaceMethodDynamic receiver toDynamic");
    var convertedRes = Eval.typecheckConvert(xx, loc, argType, split.methodArgs());
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeInterfaceMethodDynamic typecheckConvert");
    var consumeRes = GenAspectConsumeDynamic.get(xx, loc, argType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var argDynRes =
        consumeRes
            .requireValue("TypeInterfaceMethodDynamic AspectConsumeDynamic.get")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (argDynRes.hasErrors()) return argDynRes.reerr();
    var argDyn = argDynRes.requireValue("TypeInterfaceMethodDynamic argDyn");

    var descObject = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    var methodDesc = JavaMethodDescriptor.fromParts(descObject, TList.of(descObject));

    var seq = new JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(ifaceInternalName)));
    seq.add(argDyn);
    var hostName = method.hostMethodName.optUnwrapOr(method.name);
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, ifaceInternalName, hostName, methodDesc));
    if (retType.optIsNone()) {
      // No return value: the interface host method still hands back an Object on
      // the stack (its JVM signature returns Object), so discard it.
      seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
      return EResult.ok(
          new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
    }
    var retTypeVal = retType.optUnwrap();
    var retConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, retTypeVal);
    if (retConsumeOpt.optIsSome()) {
      var retDesc = retConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
      if (!retDesc.value().equals(descObject.value())) {
        var c = retDesc.value().charAt(0);
        if (c == 'L' || c == '[') {
          seq.add(JavaBytecodeUtils.cast(loc, retDesc));
        }
      }
    }
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retTypeVal);
    if (retStackRes.hasErrors()) return retStackRes.reerr();
    return EResult.ok(
        retStackRes
            .requireValue("TypeInterfaceMethodDynamic AspectStackValueDynamic.get")
            .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
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
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_INTERFACE_METHOD_DYNAMIC_IDENTITY, TypeInterfaceMethodDynamic.class);
    return oOpt.optIsSome() && iface.identity.equals(oOpt.optUnwrap().iface.identity) && method.name.equals(oOpt.optUnwrap().method.name);
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

  @Override
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return Type.exportArtifact(xx, loc, this);
  }
}
