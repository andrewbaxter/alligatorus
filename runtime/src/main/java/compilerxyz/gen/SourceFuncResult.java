package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.util.Opt;

@BuiltinPushStruct
public final class SourceFuncResult {
    public final TypeSourceFunctionConst func;

    public SourceFuncResult(TypeSourceFunctionConst func) {
        this.func = func;
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceFuncResult();
    }

    public void set_description(ExprContext xx, String description) {
        if (this.func.data().description.optIsSome()) {
            throw new IllegalStateException("Function already has a description");
        }
        this.func.data().description = Opt.some(description);
    }

    public void impl(ExprContext xx, Type arg, Opt<Type> ret, StagedExprCell implBody) {
        var data = this.func.data();
        if (data.argType.optIsSome()) {
            throw new IllegalStateException("Function already has an arg type");
        }
        data.argType = Opt.some(arg);
        if (data.retType.optIsSome()) {
            throw new IllegalStateException("Function already has a ret type");
        }
        data.retType = ret;
        if (data.implExpr.optIsSome()) {
            throw new IllegalStateException("Function already has an implementation");
        }
        data.implExpr = Opt.some((Value) implBody.value());
    }

    public void set_exception_type(ExprContext xx, Type exceptionType) {
        if (this.func.data().exceptionType.optIsSome()) {
            throw new IllegalStateException("Function already has an exception type");
        }
        this.func.data().exceptionType = Opt.some(exceptionType);
    }
}
