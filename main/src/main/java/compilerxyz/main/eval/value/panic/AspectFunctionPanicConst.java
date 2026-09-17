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
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

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
        var msgRes = DynUtil.materialize(xx, loc, converted);
        if (msgRes.hasErrors()) return msgRes.reerr();
        var panicInternal = JavaInternalName.fromClass(DynPanic.class);
        var ctorDesc = JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_VOID, TList.of(JavaDataDescriptor.fromClass(String.class)));
        var b = new JavaBytecodeBuilder(loc);
        var panic = b.newInstance(panicInternal, ctorDesc, b.reg(msgRes.requireValue("panic message")));
        b.athrow(panic);
        var throwCode = b.code();
        return EResult.ok(new CodeValue(Code.target(throwCode), EvalOutcome.CONTROL_ENDED));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
