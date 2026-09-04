package compilerxyz.main.eval.binding;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;

public record BindingSetterConsumed() implements BindingSetter {
    @Override
    public boolean bindingSetterSet(ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified) {
        return false;
    }
}
