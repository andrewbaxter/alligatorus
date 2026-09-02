package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.source_object.SourceDataMethodData;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6348L)
public final class SourceObjectMethodResult {
    public final SourceDataMethodData method;

    public SourceObjectMethodResult(SourceDataMethodData method) {
        this.method = method;
    }

    public void impl(Type arg, Opt<Type> ret, StagedExprCell implBody) {
        if (this.method.argType.optIsSome()) {
            throw new IllegalStateException("Method already has an arg type");
        }
        this.method.argType = Opt.some(arg);
        if (this.method.retType.optIsSome()) {
            throw new IllegalStateException("Method already has a ret type");
        }
        this.method.retType = ret;
        if (this.method.implExpr.optIsSome()) {
            throw new IllegalStateException("Method already has an implementation");
        }
        this.method.implExpr = Opt.some((Value) implBody.value());
    }
}
