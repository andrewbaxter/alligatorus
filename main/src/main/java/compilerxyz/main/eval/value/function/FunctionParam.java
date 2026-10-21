package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

@BuiltinFull(uniqueId = 6493L)
public record FunctionParam(String name, Type type) {

    public static Type argType(TList<FunctionParam> params) {
        if (params.isEmpty()) {
            return new Tuple(IList.of());
        }
        if (params.size() == 1) {
            return params.get(0).type();
        }
        var fields = new TOrderedMap<ConstKey, RecordField>();
        for (var p : params) {
            fields.put(new ConstKey.Str(p.name()), new RecordField.Dynamic(p.type()));
        }
        return new Record(IOrderedMap.from(fields));
    }
}
