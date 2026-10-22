package compilerxyz.main.eval;

import compilerxyz.main.gen.Target;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6001L)
@BuiltinPath("types.expr_context")
public final class ExprContext {
    public final Opt<Type> currentExceptionType;
    public final ImportStateId importState;
    public final Scope scope;
    public final Opt<QExprId> site;

    public final Target target;
    public final EvalContext vx;

    public ExprContext(
            Scope scope,
            Target target,
            EvalContext vx,
            ImportStateId importState,
            Opt<Type> currentExceptionType,
            Opt<QExprId> site) {
        this.scope = scope;
        this.target = target;
        this.vx = vx;
        this.importState = importState;
        this.currentExceptionType = currentExceptionType;
        this.site = site;
    }
}
