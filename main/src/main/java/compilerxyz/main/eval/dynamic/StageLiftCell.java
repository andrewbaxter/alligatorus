package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Target;
import compilerxyz.prelude.util.Opt;

public record StageLiftCell(
        QExprId loc,
        EvalContext vx,
        ImportStateId importState,
        Scope scope,
        Type sourceType,
        Opt<Type> currentExceptionType,
        Target target) {}
