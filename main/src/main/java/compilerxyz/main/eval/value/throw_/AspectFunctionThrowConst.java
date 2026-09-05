package compilerxyz.main.eval.value.throw_;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.ScopeChains;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynException;
import compilerxyz.main.eval.dynamic.JavaBytecodeGuarded;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectFunctionThrowConst(TypeThrowConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var erb = new Erb(loc);
        if (xx.currentExceptionType.optIsNone()) {
            return EResult.err(loc, "throw not allowed: enclosing function did not declare an exception type");
        }
        var excType = xx.currentExceptionType.optUnwrap();
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, excType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, excType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var valueDynRes = consumeRes
                .requireValue("TypeThrowConst::call consume")
                .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (valueDynRes.hasErrors()) return valueDynRes.reerr();
        var valueCode = valueDynRes.requireValue("TypeThrowConst::call toDynamic");
        var excInternal = JavaInternalName.fromClass(DynException.class);
        var ctorDesc = JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_VOID, TList.of(JavaDataDescriptor.fromClass(Object.class)));
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.newInstance(loc, excInternal));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        seq.add(valueCode);
        seq.add(JavaBytecodeUtils.invokeSpecial(loc, excInternal, "<init>", ctorDesc));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
        var throwCode = new CodeDynamic(loc, seq);
        var destRes = ScopeChains.excDest(xx, loc);
        if (destRes.hasErrors()) return destRes.reerr();
        var dest = destRes.requireValue("throw exc dest");
        Code outCode;
        if (dest.optIsSome()) {
            outCode = Code.target(new CodeDynamic(loc, new JavaBytecodeGuarded(dest.optUnwrap(), throwCode)));
        } else {
            outCode = Code.target(throwCode);
        }
        return EResult.ok(new CodeValue(outCode, EvalOutcome.CONTROL_ENDED));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
