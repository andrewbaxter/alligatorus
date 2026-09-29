package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

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
        var receiverDynRes =
                DynUtil.materialize(xx, loc, receiverRes.requireValue("TypeInterfaceMethodDynamic receiver"));
        if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
        var convertedRes = Eval.typecheckConvert(xx, loc, argType, split.methodArgs());
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var hostArgType = DynUtil.substituteBoxed(xx, argType);
        var argDynRes = DynUtil.materialize(
                xx, loc, hostArgType, convertedRes.requireValue("TypeInterfaceMethodDynamic typecheckConvert"));
        if (argDynRes.hasErrors()) return argDynRes.reerr();

        var descObject = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
        var methodDesc = JavaMethodDescriptor.fromParts(descObject, TList.of(descObject));

        var b = new JavaBytecodeBuilder(loc);
        var receiver = b.checkcast(
                JavaDataDescriptor.fromInternalName(ifaceInternalName),
                b.reg(receiverDynRes.requireValue("TypeInterfaceMethodDynamic receiver toDynamic")));
        var arg = b.reg(argDynRes.requireValue("TypeInterfaceMethodDynamic argDyn"));
        var hostName = type.method().hostMethodName.optUnwrapOr(type.method().name);
        var out = b.invokeInterface(ifaceInternalName, hostName, methodDesc, receiver, arg);
        if (retType.optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
        }
        var retTypeVal = retType.optUnwrap();
        var hostRetType = DynUtil.substituteBoxed(xx, retTypeVal);
        var retDesc = JvmSort.descriptor(xx, hostRetType);
        if (!retDesc.value().equals(descObject.value())) {
            out = b.checkcast(retDesc, out);
        }
        var hostRetCv = DynUtil.regToValue(b.code(), out, hostRetType);
        return Eval.typecheckConvert(xx, loc, retTypeVal, hostRetCv);
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
