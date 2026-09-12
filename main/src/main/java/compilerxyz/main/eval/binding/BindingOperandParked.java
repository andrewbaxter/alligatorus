package compilerxyz.main.eval.binding;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.util.TList;

public record BindingOperandParked(TList<Slot> slots) implements Binding {
    public record Slot(Type type, Binding shadow) {}

    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        throw new IllegalStateException("BindingOperandParked: a parked operand is never resolved by name");
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        var codes = new TList<Code>(slots.size());
        for (int i = slots.size() - 1; i >= 0; i--) {
            var dropRes = slots.get(i).shadow().bindingDrop(xx, loc);
            if (dropRes.hasErrors()) return dropRes.reerr();
            codes.add(dropRes.requireValue("BindingOperandParked slot drop"));
        }
        return xx.target.targetSeq(loc, codes);
    }

    @Override
    public Binding bindingFork() {
        var forked = new TList<Slot>(slots.size());
        for (var slot : slots) forked.add(new Slot(slot.type(), slot.shadow().bindingFork()));
        return new BindingOperandParked(forked);
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        var merged = new TList<Slot>(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            var otherShadows = new TList<Binding>(others.size());
            for (var other : others) {
                if (!(other instanceof BindingOperandParked op) || op.slots.size() != slots.size()) {
                    return EResult.err(loc, "BindingOperandParked: branch parked operand shape mismatch");
                }
                otherShadows.add(op.slots.get(i).shadow());
            }
            var res = slots.get(i).shadow().bindingMerge(xx, loc, otherShadows);
            if (res.hasErrors()) return res.reerr();
            merged.add(new Slot(slots.get(i).type(), res.requireValue("BindingOperandParked slot merge")));
        }
        return EResult.ok(new BindingOperandParked(merged));
    }
}
