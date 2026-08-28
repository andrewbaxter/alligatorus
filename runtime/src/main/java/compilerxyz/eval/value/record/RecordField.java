package compilerxyz.eval.value.record;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;

public sealed interface RecordField {
    record Dynamic(Type type) implements RecordField {}

    record EvalRes(EResult<CodeValue> result) implements RecordField {}
}
