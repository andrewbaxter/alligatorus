package compilerxyz.main.eval;

import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6488L)
public sealed interface EvalOutcome {
    @BuiltinFull(uniqueId = 6500L)
    record ControlEnded() implements EvalOutcome {}

    @BuiltinFull(uniqueId = 6501L)
    record NoValue() implements EvalOutcome {}

    @BuiltinFull(uniqueId = 6502L)
    record HasValue(Value value) implements EvalOutcome {}

    ControlEnded CONTROL_ENDED = new ControlEnded();
    NoValue NO_VALUE = new NoValue();

    static EvalOutcome ofValue(Value v) {
        return new HasValue(v);
    }
}
