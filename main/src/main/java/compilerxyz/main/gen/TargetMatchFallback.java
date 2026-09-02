package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6352L)
public final class TargetMatchFallback {
    public final BindingId payloadBinding;
    public final CodeValue body;

    public TargetMatchFallback(BindingId payloadBinding, CodeValue body) {
        this.payloadBinding = payloadBinding;
        this.body = body;
    }
}
