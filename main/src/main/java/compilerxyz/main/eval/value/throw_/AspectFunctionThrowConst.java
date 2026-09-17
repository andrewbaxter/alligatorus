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
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeGuarded;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

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
        var payloadRes = DynUtil.materialize(xx, loc, converted);
        if (payloadRes.hasErrors()) return payloadRes.reerr();
        var excInternal = JavaInternalName.fromClass(DynException.class);
        var ctorDesc = JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_VOID, TList.of(JavaDataDescriptor.fromClass(Object.class)));
        var b = new JavaBytecodeBuilder(loc);
        var exc = b.newInstance(excInternal, ctorDesc, b.reg(payloadRes.requireValue("throw payload")));
        b.athrow(exc);
        var throwCode = b.code();
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
