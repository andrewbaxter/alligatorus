package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record FunctionParam(String name, Type type) {

    public static Type argType(TList<FunctionParam> params) {
        if (params.isEmpty()) {
            return new Tuple(Opt.none(), TList.<TupleElement>of());
        }
        if (params.size() == 1) {
            return params.get(0).type();
        }
        var fields = new TOrderedMap<ConstKey, RecordField>();
        for (var p : params) {
            fields.put(new ConstKey.Str(p.name()), new RecordField.Dynamic(p.type()));
        }
        return new Record(Opt.none(), fields);
    }
}
