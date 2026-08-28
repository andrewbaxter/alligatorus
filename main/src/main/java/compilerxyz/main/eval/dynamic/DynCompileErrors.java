package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.prelude.util.TList;

public final class DynCompileErrors extends RuntimeException {
    public final TList<ExprErr> errors;

    public DynCompileErrors(TList<ExprErr> errors) {
        super(errors.toString());
        this.errors = errors;
    }
}
