package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6344L)
public final class SourceFuncResult {
    public final TypeSourceFunctionConst func;

    public SourceFuncResult(TypeSourceFunctionConst func) {
        this.func = func;
    }

    public void set_description(String description) {
        if (this.func.data().description.optIsSome()) {
            throw new IllegalStateException("Function already has a description");
        }
        this.func.data().description = Opt.some(description);
    }

    public void impl(Type arg, Opt<Type> ret, StagedExprCell implBody) {
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

    public void set_exception_type(Type exceptionType) {
        if (this.func.data().exceptionType.optIsSome()) {
            throw new IllegalStateException("Function already has an exception type");
        }
        this.func.data().exceptionType = Opt.some(exceptionType);
    }
}
