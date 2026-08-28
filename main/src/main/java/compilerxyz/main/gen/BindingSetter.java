package compilerxyz.main.gen;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 2008L, scopeId = 3008)
@BuiltinPath("eval.binding_setter")
public interface BindingSetter {
    boolean bindingSetterSet(ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified);
}
