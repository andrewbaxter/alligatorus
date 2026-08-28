package compilerxyz.eval.dynamic;

import compilerxyz.eval.ExprErr;
import compilerxyz.util.TList;

public final class DynCompileErrors extends RuntimeException {
    public final TList<ExprErr> errors;

    public DynCompileErrors(TList<ExprErr> errors) {
        super(errors.toString());
        this.errors = errors;
    }
}
