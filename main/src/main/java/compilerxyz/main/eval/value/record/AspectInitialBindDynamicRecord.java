package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectInitialBindDynamicRecord(Record type) implements AspectInitialBindDynamic {

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
        for (var entry : type.fields.entrySet()) {
            scopeFields.put(entry.getKey(), new RecordScopeField.Dynamic(Record.recordFieldType(entry.getValue())));
        }
        return new BindingRecord(scopeFields, Opt.some(dynamicKey));
    }
}
