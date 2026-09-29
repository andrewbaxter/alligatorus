package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectRecord;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectRecordRecord(Record type) implements AspectRecord {

    @Override
    public TOrderedMap<ConstKey, Type> aspectRecordFieldTypes() {
        var out = new TOrderedMap<ConstKey, Type>();
        for (var entry : type.fields()) out.put(entry.getKey(), Record.recordFieldType(entry.getValue()));
        return out;
    }

    @Override
    public TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(ExprContext xx, QExprId loc, Value value) {
        if (value instanceof ValueDynamic vd) {
            var runtimeRecord = MustCast.mustCast(Record.class, vd.type());
            var runtimeCv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(vd));
            var read = new TOrderedMap<ConstKey, EResult<CodeValue>>();
            for (var key : runtimeRecord.fields().keys()) {
                read.put(key, runtimeRecord.accessOrDrop(xx, loc, runtimeCv, Opt.some(key)));
            }
            return read;
        }
        var r = MustCast.mustCast(Record.class, value);
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
