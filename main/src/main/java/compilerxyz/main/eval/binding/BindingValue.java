package compilerxyz.main.eval.binding;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.Value;
import compilerxyz.main.meta.Meta;
import compilerxyz.prelude.util.TList;

public record BindingValue(Value value) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
    }

    @Override
    public Meta bindingMeta(ExprContext xx) {
        return value.valueType().typeMeta(xx);
    }

    @Override
    public Binding bindingFork() {
        return this;
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        return EResult.ok(this);
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return Binding.bindingDrop(xx, loc, this);
    }
}
