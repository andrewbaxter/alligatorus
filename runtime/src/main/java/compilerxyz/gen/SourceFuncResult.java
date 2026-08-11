package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import java.lang.IllegalStateException;
import java.lang.String;

@BuiltinPushStruct
public final class SourceFuncResult {
    public final TypeSourceFunctionConst func;

    public SourceFuncResult(TypeSourceFunctionConst func) {
        this.func = func;
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceFuncResult();
    }

    public TypeSourceFunctionConst get_ref() {
        return this.func;
    }

    public void set_description(ExprContext xx, String description) {
        if (this.func.data().description != null) {
            throw new IllegalStateException("Function already has a description");
        }
        this.func.data().description = description;
    }

    public void impl(
        ExprContext xx, Type arg, compilerxyz.util.Opt<Type> ret, StagedExprCell implBody) {
        var data = this.func.data();
        if (data.argType != null) {
            throw new IllegalStateException("Function already has an arg type");
        }
        data.argType = arg;
        if (data.retType instanceof compilerxyz.util.Opt.Some) {
            throw new IllegalStateException("Function already has a ret type");
        }
        data.retType = ret;
        if (data.implExpr != null) {
            throw new IllegalStateException("Function already has an implementation");
        }
        data.implExpr = (Value) implBody.value();
    }

    public void set_exception_type(ExprContext xx, Type exceptionType) {
        if (this.func.data().exceptionType != null) {
            throw new IllegalStateException("Function already has an exception type");
        }
        this.func.data().exceptionType = exceptionType;
    }
}
