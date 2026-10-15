package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.DynCompileErrors;
import compilerxyz.main.eval.dynamic.DynException;
import compilerxyz.main.eval.dynamic.DynPanic;
import java.lang.reflect.InvocationTargetException;

public final class EvalRoot {
    private EvalRoot() {}

    public static <T> EResult<T> thrownErrors(
            DefinitionSetClassLoader loader, QExprId loc, InvocationTargetException e, String what) {
        var cause = e.getCause();
        if (cause == null) {
            throw new AssertionError(what + " threw without a cause", e);
        }
        if (cause instanceof DynCompileErrors compileErrs) {
            return EResult.errs(compileErrs.errors);
        }
        var at = loc;
        for (var frame : cause.getStackTrace()) {
            var found = loader.locate(frame);
            if (found.optIsSome()) {
                at = found.optUnwrap();
                break;
            }
        }
        if (cause instanceof DynPanic panic) {
            return EResult.err(at, "panic: " + panic.getMessage());
        }
        if (cause instanceof DynException dynEx) {
            return EResult.err(at, dynEx.payload == null ? "null" : dynEx.payload.toString());
        }
        var trace = new StringBuilder("uncaught ").append(cause);
        for (var frame : cause.getStackTrace()) trace.append("\n    at ").append(frame);
        return EResult.err(at, trace.toString());
    }
}
