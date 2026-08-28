package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;

@BuiltinFull(uniqueId = 2008L, scopeId = 3008)
@BuiltinPath("eval.binding_setter")
public interface BindingSetter {
    boolean bindingSetterSet(ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified);
}
