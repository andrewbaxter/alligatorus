package compilerxyz.eval;

import compilerxyz.gen.Value;

public sealed interface EvalOutcome {
    record ControlEnded() implements EvalOutcome {}

    record NoValue() implements EvalOutcome {}

    record HasValue(Value value) implements EvalOutcome {}

    ControlEnded CONTROL_ENDED = new ControlEnded();
    NoValue NO_VALUE = new NoValue();

    static EvalOutcome ofValue(Value v) {
        return new HasValue(v);
    }
}
