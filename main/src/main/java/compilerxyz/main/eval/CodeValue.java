package compilerxyz.main.eval;

import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 1041L)
@BuiltinPath("types.cve")
public record CodeValue(Code code, EvalOutcome outcome) {

    public Opt<Value> value() {
        return switch (outcome) {
            case EvalOutcome.HasValue h -> Opt.some(h.value());
            case EvalOutcome.ControlEnded c -> Opt.none();
            case EvalOutcome.NoValue n -> Opt.none();
        };
    }

    public boolean controlEnded() {
        return switch (outcome) {
            case EvalOutcome.ControlEnded c -> true;
            case EvalOutcome.HasValue h -> false;
            case EvalOutcome.NoValue n -> false;
        };
    }
}
