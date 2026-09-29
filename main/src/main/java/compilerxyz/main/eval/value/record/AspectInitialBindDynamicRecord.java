package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectInitialBindDynamicRecord(Record type) implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
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
        return new BindingRecord(scopeFields, Opt.some(dynamicKey));
    }
}
