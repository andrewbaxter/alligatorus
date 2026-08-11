package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_object.SourceDataMethodData;
import java.lang.IllegalStateException;

@BuiltinPushStruct
public final class SourceObjectMethodResult {
    public final SourceDataMethodData method;

    public SourceObjectMethodResult(SourceDataMethodData method) {
        this.method = method;
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceObjectMethodResult();
    }

    public void impl(
        ExprContext xx, Type arg, compilerxyz.util.Opt<Type> ret, StagedExprCell implBody) {
        if (this.method.argType != null) {
            throw new IllegalStateException("Method already has an arg type");
        }
        this.method.argType = arg;
        if (this.method.retType instanceof compilerxyz.util.Opt.Some) {
            throw new IllegalStateException("Method already has a ret type");
        }
        this.method.retType = ret;
        if (this.method.implExpr != null) {
            throw new IllegalStateException("Method already has an implementation");
        }
        this.method.implExpr = (Value) implBody.value();
    }
}
