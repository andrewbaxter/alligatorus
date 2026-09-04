package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.util.TOrderedMap;

record BindingSetterArgumentField(TOrderedMap<ConstKey, ArgumentEntry> fields, ConstKey key, BindingSetter parent)
        implements BindingSetter {
    @Override
    public boolean bindingSetterSet(ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified) {
        var replaced = new TOrderedMap<ConstKey, ArgumentEntry>();
        for (var entry : fields.entrySet()) {
            replaced.put(
                    entry.getKey(),
                    entry.getKey().equals(key)
                            ? new ArgumentEntry(newBinding, entry.getValue().type())
                            : entry.getValue());
        }
        return parent.bindingSetterSet(xx, loc, new ArgumentRecordBinding(replaced), dropModified);
    }
}
