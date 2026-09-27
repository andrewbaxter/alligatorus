package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectRecord;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectRecordRecord(Record type) implements AspectRecord {

    @Override
    public TOrderedMap<ConstKey, Type> aspectRecordFieldTypes() {
        var out = new TOrderedMap<ConstKey, Type>();
        for (var entry : type.fields()) {
            switch (entry.getValue()) {
                case RecordField.Dynamic d -> out.put(entry.getKey(), d.type());
                case RecordField.EvalRes er -> {
                    var res = er.result();
                    if (res.hasErrors()) {
                        throw new IllegalStateException("Record::recordFieldTypes: field '"
                                + entry.getKey().constKeyToCanonical() + "' EvalRes has errors");
                    }
                    var cv = res.requireValue("Record::recordFieldTypes field");
                    if (cv.value().optIsNone()) {
                        throw new IllegalStateException("Record::recordFieldTypes: field '"
                                + entry.getKey().constKeyToCanonical() + "' EvalRes has no compile-time value");
                    }
                    var v = cv.value().optUnwrap();
                    if (v instanceof Type asType) {
                        out.put(entry.getKey(), asType);
                    } else if (v instanceof ValueObjectConst voc && voc.data() instanceof Type asDataType) {
                        out.put(entry.getKey(), asDataType);
                    } else {
                        throw new IllegalStateException("Record::recordFieldTypes: field '"
                                + entry.getKey().constKeyToCanonical() + "' value is not a Type: " + v.getClass());
                    }
                }
            }
        }
        return out;
    }

    @Override
    public TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(ExprContext xx, QExprId loc, Value value) {
        if (!(value instanceof Record r)) {
            throw new IllegalArgumentException(
                    "Record::recordTakeFields: expected Record value, got " + value.getClass());
        }
        var out = new TOrderedMap<ConstKey, EResult<CodeValue>>();
        for (var entry : r.fields()) {
            RecordField.EvalRes er;
            switch (entry.getValue()) {
                case RecordField.EvalRes erVal -> er = erVal;
                case RecordField.Dynamic d ->
                    throw new IllegalStateException("Record::recordTakeFields: field '"
                            + entry.getKey().constKeyToCanonical() + "' is not EvalRes-form");
            }
            out.put(entry.getKey(), er.result());
        }
        return out;
    }
}
