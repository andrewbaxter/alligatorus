package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectFieldUseDynamicRecord(Record type) implements AspectFieldUseDynamic {

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        var fieldCode = Code.none(loc);
        var newFields = new TOrderedMap<ConstKey, RecordField>();
        for (var entry : type.fields()) {
            var t =
                    switch (entry.getValue()) {
                        case RecordField.Dynamic d -> d.type();
                        case RecordField.EvalRes er ->
                            throw new IllegalStateException(
                                    "Record::fieldUseDynamic: field-type Record has non-Dynamic entry '"
                                            + entry.getKey().constKeyToCanonical() + "'");
                    };
            newFields.put(entry.getKey(), new RecordField.Dynamic(t));
        }
        Value innerRecord = new ValueDynamic(source, new Record(IOrderedMap.from(newFields)));
        return EResult.ok(new CodeValue(fieldCode, EvalOutcome.ofValue(innerRecord)));
    }
}
