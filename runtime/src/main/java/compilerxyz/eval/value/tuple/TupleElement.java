package compilerxyz.eval.value.tuple;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

public sealed interface TupleElement {
  record Dynamic(Type type) implements TupleElement {}

  record EvalRes(EResult<CodeValue> result) implements TupleElement {}
}
