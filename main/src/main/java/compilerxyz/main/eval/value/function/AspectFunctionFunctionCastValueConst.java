package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionFunctionCastValueConst implements AspectFunction {
    public static final AspectFunctionFunctionCastValueConst INSTANCE = new AspectFunctionFunctionCastValueConst();

    private AspectFunctionFunctionCastValueConst() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        return EResult.err(
                loc,
                "TypeFunctionCastValueConst::call: needs ExprContext externals + TypeSourceObject"
                        + " {Const,Dynamic} + CAST_VALUE_FN_SCOPE_ID emission (deferred)");
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
