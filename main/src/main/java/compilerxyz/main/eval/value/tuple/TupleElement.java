package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Type;

public sealed interface TupleElement {
    record Dynamic(Type type) implements TupleElement {}

    record EvalRes(EResult<CodeValue> result) implements TupleElement {}
}
