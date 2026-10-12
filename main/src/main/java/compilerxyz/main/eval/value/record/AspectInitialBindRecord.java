package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.BindingSlot;
import compilerxyz.main.eval.BindingSlotDynamic;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectInitialBindRecord(Record type) implements AspectInitialBind {

    @Override
    public EResult<Binding> aspectInitialBindInitialBind(ExprContext xx, QExprId loc, BindingSlot slot) {
        var dynamicKey = BindingSlotDynamic.idOf(slot);
        var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
        for (var entry : type.fields()) {
            scopeFields.put(
                    entry.getKey(),
                    switch (entry.getValue()) {
                        case RecordField.Dynamic d -> new RecordScopeField.Dynamic(d.type());
                        case RecordField.EvalRes held ->
                            new RecordScopeField.Scope(new BindingValue(held.result()
                                    .requireValue("Record::initialBindDynamic held field")
                                    .value()
                                    .optUnwrap()));
                    });
        }
        return EResult.ok(new BindingRecord(scopeFields, Opt.some(dynamicKey)));
    }
}
