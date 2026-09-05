package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectFunctionInterfaceMethodDynamic(TypeInterfaceMethodDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argType = type.method().argType;
        var retType = type.method().retType;
        var ifaceInternalName = type.iface().jvmInternalName(xx);

        var splitRes = DynUtil.methodRefSplitArgument(
                xx, loc, argType, argument, "interface method '" + type.method().name + "'");
        if (splitRes.hasErrors()) return splitRes.reerr();
        var split = splitRes.requireValue("TypeInterfaceMethodDynamic split");
        var ifaceType = new TypeSourceInterfaceDynamic(type.iface());
        var receiverRes = Eval.typecheckConvert(xx, loc, ifaceType, split.receiver());
        if (receiverRes.hasErrors()) return receiverRes.reerr();
        var receiverDynRes = GenAspectConsumeDynamic.getMaybe(xx, ifaceType)
                .optUnwrap()
                .aspectConsumeDynamicToDynamic(
                        xx, loc, receiverRes.requireValue("TypeInterfaceMethodDynamic receiver"));
        if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
        var selfDyn = receiverDynRes.requireValue("TypeInterfaceMethodDynamic receiver toDynamic");
        var convertedRes = Eval.typecheckConvert(xx, loc, argType, split.methodArgs());
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var converted = convertedRes.requireValue("TypeInterfaceMethodDynamic typecheckConvert");
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, argType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var argDynRes = consumeRes
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
        var hostName = type.method().hostMethodName.optUnwrapOr(type.method().name);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, ifaceInternalName, hostName, methodDesc));
        if (retType.optIsNone()) {
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
            return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
        }
        var retTypeVal = retType.optUnwrap();
        var retDesc = JvmSort.descriptor(xx, retTypeVal);
        if (retDesc.isReference() && !retDesc.value().equals(descObject.value())) {
            seq.add(JavaBytecodeUtils.cast(loc, retDesc));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retTypeVal);
        if (retStackRes.hasErrors()) return retStackRes.reerr();
        return EResult.ok(retStackRes
                .requireValue("TypeInterfaceMethodDynamic AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
