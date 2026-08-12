package compilerxyz.eval.value.record;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

public sealed interface RecordField {
  record Dynamic(Type type) implements RecordField {}

  record EvalRes(EResult<CodeValue> result) implements RecordField {
    public boolean isUnreachable() {
      if (result.hasErrors()) return false;
      var cv = result.valueOrNull();
      if (cv.optIsNone()) return false;
      return cv.optUnwrap().value().optIsNone();
    }
  }
}
