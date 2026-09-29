package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public final class TypeMethodShared {
    private TypeMethodShared() {}

    public static EResult<CodeValue> callDynamic(
            ExprContext xx,
            QExprId loc,
            CodeValue self,
            CodeValue argument,
            String what,
            Type argType,
            Opt<Type> ret,
            JavaInternalName klass,
            String methodName,
            JavaMethodDescriptor descriptor,
            boolean isInterface,
            boolean needsEvalContext) {
        var receiverDynRes = DynUtil.valueReg(loc, self);
        if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
        EResult<DynUtil.Positional> argsDynRes;
        if (DynUtil.zeroArgSignature(xx, argType)) {
            argsDynRes = EResult.ok(new DynUtil.Positional(Code.none(loc), new TList<>()));
        } else {
            var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
            if (convertedRes.hasErrors()) return convertedRes.reerr();
            var converted = convertedRes.requireValue(what + "::call converted arg");
            argsDynRes = DynUtil.destructurePositional(xx, loc, argType, converted);
            if (argsDynRes.hasErrors()) return argsDynRes.reerr();
        }
        var b = new JavaBytecodeBuilder(loc);
        var regs = new TList<JavaBytecodeReg>();
        regs.add(b.reg(receiverDynRes.requireValue("receiverDyn")));
        if (needsEvalContext) {
            regs.add(b.reg(EvalRootEmitter.loadEvalContext(loc, xx)));
        }
        var positional = argsDynRes.requireValue(what + "::call args");
        var carryAdd = b.addCode(loc, positional.carry());
        if (carryAdd.optIsSome()) return EResult.errs(carryAdd.optUnwrap());
        var args = positional.args();
        var paramDescs = descriptor.argumentDescriptors();
        var paramOffset = needsEvalContext ? 1 : 0;
        for (int i = 0; i < args.size(); i++) {
            var reg = b.reg(args.get(i));
            var pi = i + paramOffset;
            if (pi < paramDescs.size()) {
                var expected = paramDescs.get(pi);
                if (expected.value().startsWith("L") && !expected.value().equals("Ljava/lang/Object;")) {
                    reg = b.checkcast(expected, reg);
                }
            }
            regs.add(reg);
        }
        var out = b.invoke(
                isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                klass,
                methodName,
                descriptor,
                isInterface,
                regs);
        if (ret.optIsNone() || out.optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
        }
        return EResult.ok(DynUtil.regToValue(b.code(), out.optUnwrap(), ret.optUnwrap()));
    }
}
