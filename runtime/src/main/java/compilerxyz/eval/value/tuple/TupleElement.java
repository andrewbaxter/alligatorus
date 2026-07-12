package compilerxyz.eval.value.tuple;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

public sealed interface TupleElement {
  record Dynamic(Type type) implements TupleElement {}

  record EvalRes(EResult<CodeValue> result) implements TupleElement {
    public boolean isUnreachable() {
      if (result.hasErrors()) return false;
      var cv = result.valueOrNull();
      if (cv == null) return false;
      return !(cv.value() instanceof Opt.Some<Value>);
    }
  }
}
