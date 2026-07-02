package compilerxyz.eval.dynamic;

import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Type;
import org.jspecify.annotations.Nullable;

public record StageLiftCell(
    QExprId loc,
    EvalContext vx,
    ImportStateId importState,
    Scope scope,
    Type sourceType,
    @Nullable Type currentExceptionType,
    Target target) {}
