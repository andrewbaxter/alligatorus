package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6411L)
public sealed interface RecordField {
    record Dynamic(Type type) implements RecordField {}

    record EvalRes(EResult<CodeValue> result) implements RecordField {}
}
