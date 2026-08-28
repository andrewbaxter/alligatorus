package compilerxyz.main.eval.binding;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record BindingObjectConst(Type type, Object data) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
        return new CodeValue(Code.none(loc), EvalOutcome.ofValue(new ValueObjectConst(type, data)));
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
                        loc, "cannot merge BindingObjectConst with a differently-typed binding across branches");
            }
        }
        return EResult.ok(this);
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return Binding.bindingDrop(xx, loc, this);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return Binding.elementUseDynamicBind(dynamicKey);
    }
}
