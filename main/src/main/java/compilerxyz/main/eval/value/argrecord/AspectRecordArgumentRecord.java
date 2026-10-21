package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectRecord;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectRecordArgumentRecord(TypeArgumentRecord type) implements AspectRecord {

    @Override
    public TOrderedMap<ConstKey, Type> aspectRecordFieldTypes() {
        var out = new TOrderedMap<ConstKey, Type>();
        for (var entry : type.fieldTypes().entrySet()) out.put(entry.getKey(), entry.getValue());
        return out;
    }

    @Override
    public TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(ExprContext xx, QExprId loc, Value value) {
        var record = MustCast.mustCast(ArgumentRecord.class, value);
        var out = new TOrderedMap<ConstKey, EResult<CodeValue>>();
        for (var entry : record.fields().entrySet()) {
            var key = entry.getKey();
            out.put(
                    key,
                    EResult.ok(entry.getValue()
                            .binding()
                            .bindingUse(
                                    xx,
                                    loc,
                                    new BindingSetterArgumentField(record.fields(), key, record.setter()),
                                    Code.none(loc))));
        }
        return out;
    }
}
