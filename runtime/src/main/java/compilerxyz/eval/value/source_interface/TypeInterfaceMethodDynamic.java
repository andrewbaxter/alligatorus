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

@BuiltinLight(uniqueId = 6136L)
public record TypeInterfaceMethodDynamic(InterfaceData iface, InterfaceMethodData method)
    implements Type, AspectConsumeDynamic, AspectFunction, AspectInitialBind, ExporterAny {

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

    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeInterfaceMethodDynamic::call: self has no dynamic code");
    }
    var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
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
    var hostName = method.hostMethodName == null ? method.name : method.hostMethodName;
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, ifaceInternalName, hostName, methodDesc));
    if (!(retType instanceof Opt.Some<Type> retSome)) {
      // No return value: the interface host method still hands back an Object on
      // the stack (its JVM signature returns Object), so discard it.
      seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
      return EResult.ok(
          new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
    }
    var retTypeVal = retSome.value();
    var retConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, retTypeVal);
    if (retConsumeOpt instanceof Opt.Some<AspectConsumeDynamic> retConsumeSome) {
      var retDesc = retConsumeSome.value().aspectConsumeDynamicJvmClass(xx);
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_INTERFACE_METHOD_DYNAMIC_IDENTITY, TypeInterfaceMethodDynamic.class) instanceof Opt.Some<TypeInterfaceMethodDynamic> oSome
        && iface.identity.equals(oSome.value().iface.identity) && method.name.equals(oSome.value().method.name);
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
