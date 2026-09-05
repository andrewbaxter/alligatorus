package compilerxyz.main.eval.value.panic;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public final class AspectFunctionPanicConst implements AspectFunction {
    public static final AspectFunctionPanicConst INSTANCE = new AspectFunctionPanicConst();

    private AspectFunctionPanicConst() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        Type stringType = new TypeStringDynamic();
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, stringType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var toDynamicOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, stringType));
        if (toDynamicOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var msgCodeOpt = erb.mustEr(toDynamicOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, converted));
        if (msgCodeOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var msgCode = msgCodeOpt.optUnwrap();
        var panicInternal = JavaInternalName.fromClass(DynPanic.class);
        var ctorDesc = JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_VOID, TList.of(JavaDataDescriptor.fromClass(String.class)));
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.newInstance(loc, panicInternal));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        seq.add(msgCode);
        seq.add(JavaBytecodeUtils.invokeSpecial(loc, panicInternal, "<init>", ctorDesc));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
        var throwCode = new CodeDynamic(loc, seq);
        return EResult.ok(new CodeValue(Code.target(throwCode), EvalOutcome.CONTROL_ENDED));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
