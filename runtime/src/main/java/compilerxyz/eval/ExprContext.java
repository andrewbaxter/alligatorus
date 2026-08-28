package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.gen.Target;
import compilerxyz.util.Opt;

@BuiltinFull(uniqueId = 6001L)
public final class ExprContext {
    public final Opt<Type> currentExceptionType;
    public final ImportStateId importState;
    public final Scope scope;
    public final Target target;
    public final EvalContext vx;

    public ExprContext(
            Scope scope, Target target, EvalContext vx, ImportStateId importState, Opt<Type> currentExceptionType) {
        this.scope = scope;
        this.target = target;
        this.vx = vx;
        this.importState = importState;
        this.currentExceptionType = currentExceptionType;
    }
}
