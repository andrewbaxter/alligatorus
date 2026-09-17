package compilerxyz.main.eval.value.binding;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;

public record BindingDynamicBinding(BindingId dynamicKey, Type type) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        Value value =
                new ValueDynamic(new ValueDynamicSource.Slot(dynamicKey, JvmSort.descriptor(xx, type), setter), type);
        return new CodeValue(code, EvalOutcome.ofValue(value));
    }

    @Override
    public Binding bindingFork() {
        return this;
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        Type mergedType = type;
        boolean merged = false;
        for (var other : others) {
            if (this.equals(other)) continue;
            if (!(other instanceof BindingDynamicBinding ob) || dynamicKey != ob.dynamicKey) {
                return EResult.err(
                        loc,
                        "cannot merge " + getClass().getSimpleName()
                                + " with a differently-typed binding across branches");
            }
            var aOpt = TypeSourceUnionDynamic.unionOf(xx, mergedType);
            var bOpt = TypeSourceUnionDynamic.unionOf(xx, ob.type);
            if (aOpt.optIsNone()
                    || bOpt.optIsNone()
                    || !aOpt.optUnwrap().identity.equals(bOpt.optUnwrap().identity)) {
                return EResult.err(
                        loc,
                        "cannot merge " + getClass().getSimpleName()
                                + " with a differently-typed binding across branches");
            }
            var mergedActive = new TList<Integer>(
                    aOpt.optUnwrap().active.size() + bOpt.optUnwrap().active.size());
            mergedActive.addAll(aOpt.optUnwrap().active);
            mergedActive.addAll(bOpt.optUnwrap().active);
            mergedType = aOpt.optUnwrap().withActiveVariants(mergedActive);
            merged = true;
        }
        if (!merged) {
            return EResult.ok(this);
        }
        return EResult.ok(new BindingDynamicBinding(dynamicKey, mergedType));
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return Binding.bindingDrop(xx, loc, this);
    }
}
