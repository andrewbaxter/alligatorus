package compilerxyz.eval.value.function;

import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Type;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public record FunctionParam(String name, Type type) {

  /**
   * The in-language argument shape for a host parameter list: no parameters take an empty
   * tuple, a single parameter is taken bare, and two or more parameters are taken as a
   * record keyed by parameter name.
   */
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
