package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.meta.Meta;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record ArgumentRecordBinding(TOrderedMap<ConstKey, ArgumentEntry> fields) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        return new CodeValue(Code.none(loc), EvalOutcome.ofValue(new ArgumentRecord(fields, setter)));
    }

    @Override
    public Meta bindingMeta(ExprContext xx) {
        return TypeArgumentRecord.of(fields).typeMeta(xx);
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
