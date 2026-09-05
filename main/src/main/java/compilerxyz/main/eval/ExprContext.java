package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.gen.Target;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

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

    public static ExprContext freshRoot(CompileImportContext ix) {
        var vx = new EvalContext(ix);
        var scope = new Scope(vx);
        var target = new TargetDynamic();
        return new ExprContext(scope, target, vx, ix.importState, Opt.some(new TypeStringConst()));
    }
}
