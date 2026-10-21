package compilerxyz.main.eval.value.binding;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.BindingDynamicSlot;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TList;

public record BindingDynamicBinding(BindingId dynamicKey, Type type) implements Binding, BindingDynamicSlot {
    @Override
    public BindingId bindingDynamicSlotKey() {
        return dynamicKey;
    }

    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        Value value = new ValueDynamic(new ValueDynamicSource.Slot(dynamicKey, setter), type);
        return new CodeValue(code, EvalOutcome.ofValue(value));
    }

    @Override
    public Binding bindingFork() {
        return this;
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        var unionOpt = TypeSourceUnionDynamic.unionOf(xx, type);
        if (unionOpt.optIsNone()) {
            return EResult.ok(this);
        }
        var union = unionOpt.optUnwrap();
        var mergedActive = new TList<Integer>();
        mergedActive.addAll(union.active);
        for (var other : others) {
            var otherType = MustCast.mustCast(BindingDynamicBinding.class, other).type;
            mergedActive.addAll(TypeSourceUnionDynamic.unionOf(xx, otherType).optUnwrap().active);
        }
        return EResult.ok(new BindingDynamicBinding(dynamicKey, union.withActiveVariants(mergedActive)));
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return Binding.bindingDrop(xx, loc, this);
    }
}
