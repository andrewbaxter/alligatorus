package compilerxyz.main.gen;

import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.prelude.annotations.BuiltinEvalContext;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6344L)
public final class SourceFuncResult {
    public final TypeSourceFunctionConst func;

    public SourceFuncResult(TypeSourceFunctionConst func) {
        this.func = func;
    }

    public Type funcType() {
        return func;
    }

    @BuiltinEvalContext
    public void impl(
            EvalContext vx,
            Type arg,
            Opt<Type> ret,
            StagedExprCell implBody,
            Opt<Type> exceptionType,
            Opt<String> description) {
        var data = this.func.data();
        DefinitionSetBuilder.requireArgBindable(
                DefinitionSetBuilder.builderContext(vx), arg, "Function '" + data.name.optUnwrapOr("(unnamed)") + "'");
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
        data.exceptionType = exceptionType;
        data.description = description;
    }
}
