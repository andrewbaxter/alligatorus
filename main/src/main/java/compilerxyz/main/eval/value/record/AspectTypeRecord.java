package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectTypeRecord(IOrderedMap<ConstKey, AspectType> fieldAspects) implements AspectType {

    @Override
    public Type aspectTypeType(Value value) {
        var record = MustCast.mustCast(Record.class, value);
        var fieldTypes = new TOrderedMap<ConstKey, RecordField>();
        for (var entry : record.fields()) {
            RecordField.EvalRes er;
            switch (entry.getValue()) {
                case RecordField.EvalRes erVal -> er = erVal;
                case RecordField.Dynamic d ->
                    throw new IllegalArgumentException("Record::type_: field '"
                            + entry.getKey().constKeyToCanonical() + "' has no compile-time value");
            }
            var fieldValue =
                    er.result().requireValue("Record::type_ field").value().optUnwrap();
            var fieldType = fieldAspects.getOpt(entry.getKey()).optUnwrap().aspectTypeType(fieldValue);
            fieldTypes.put(entry.getKey(), new RecordField.Dynamic(fieldType));
        }
        return new Record(IOrderedMap.from(fieldTypes));
    }
}
