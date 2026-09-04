package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6352L)
public final class TargetMatchFallback {
    public final Binding payloadBinding;
    public final CodeValue body;

    public TargetMatchFallback(Binding payloadBinding, CodeValue body) {
        this.payloadBinding = payloadBinding;
        this.body = body;
    }
}
