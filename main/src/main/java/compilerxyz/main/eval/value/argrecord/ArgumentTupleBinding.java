package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.util.TList;

public record ArgumentTupleBinding(ArgumentTuple tuple) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        return new CodeValue(Code.none(loc), EvalOutcome.ofValue(tuple));
    }

    @Override
    public Binding bindingFork() {
        return this;
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        for (var other : others) {
            if (!this.equals(other)) {
                return EResult.err(
                        loc,
                        "cannot merge " + getClass().getSimpleName()
                                + " with a differently-typed binding across branches");
            }
        }
        return EResult.ok(this);
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return Binding.bindingDrop(xx, loc, this);
    }
}
