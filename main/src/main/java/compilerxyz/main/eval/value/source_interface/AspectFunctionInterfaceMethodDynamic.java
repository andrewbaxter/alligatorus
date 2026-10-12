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
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectFunctionInterfaceMethodDynamic(TypeInterfaceMethodDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var method = type.method();
        var argType = method.argType;
        var retType = method.retType;
        var ifaceInternalName = type.iface().internalName();

        var splitRes =
                DynUtil.methodRefSplitArgument(xx, loc, argType, argument, "interface method '" + method.name + "'");
        if (splitRes.hasErrors()) return splitRes.reerr();
        var split = splitRes.requireValue("TypeInterfaceMethodDynamic split");
        var ifaceType = new TypeSourceInterfaceDynamic(type.iface());
        var receiverRes = Eval.typecheckConvert(xx, loc, ifaceType, split.receiver());
        if (receiverRes.hasErrors()) return receiverRes.reerr();
        var receiverDynRes =
                DynUtil.materialize(xx, loc, receiverRes.requireValue("TypeInterfaceMethodDynamic receiver"));
        if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
        EResult<DynUtil.Positional> argsRes;
        if (DynUtil.zeroArgSignature(xx, argType)) {
            argsRes = EResult.ok(new DynUtil.Positional(Code.none(loc), new TList<>()));
        } else {
            var convertedRes = Eval.typecheckConvert(xx, loc, argType, split.methodArgs());
            if (convertedRes.hasErrors()) return convertedRes.reerr();
            argsRes = DynUtil.destructurePositional(
                    xx, loc, argType, convertedRes.requireValue("TypeInterfaceMethodDynamic typecheckConvert"));
            if (argsRes.hasErrors()) return argsRes.reerr();
        }
        var positional = argsRes.requireValue("TypeInterfaceMethodDynamic args");

        var b = new JavaBytecodeBuilder(loc);
        if (type.iface().set.optIsSome()) b.defSet(type.iface().set.optUnwrap());
        var regs = new TList<JavaBytecodeReg>();
        regs.add(b.checkcast(
                JavaDataDescriptor.fromInternalName(ifaceInternalName),
                b.reg(receiverDynRes.requireValue("TypeInterfaceMethodDynamic receiver toDynamic"))));
        var carryAdd = b.addCode(loc, positional.carry());
        if (carryAdd.optIsSome()) return EResult.errs(carryAdd.optUnwrap());
        var paramDescs = method.jvmDescriptor.argumentDescriptors();
        var args = positional.args();
        for (int i = 0; i < args.size(); i++) {
            var reg = b.reg(args.get(i));
            var expected = paramDescs.get(i);
            if (expected.isReference() && !expected.value().equals("Ljava/lang/Object;")) {
                reg = b.checkcast(expected, reg);
            }
            regs.add(reg);
        }
        var out =
                b.invoke(Opcodes.INVOKEINTERFACE, ifaceInternalName, method.jvmName, method.jvmDescriptor, true, regs);
        if (retType.optIsNone() || out.optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
        }
        return EResult.ok(DynUtil.regToValue(b.code(), out.optUnwrap(), retType.optUnwrap()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
