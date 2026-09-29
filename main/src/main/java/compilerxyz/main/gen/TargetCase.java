package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6351L)
public final class TargetCase {
    public final Type variantType;
    public final int variantIndex;
    public final Binding payloadBinding;
    public final CodeValue body;

    public TargetCase(Type variantType, int variantIndex, Binding payloadBinding, CodeValue body) {
        this.variantType = variantType;
        this.variantIndex = variantIndex;
        this.payloadBinding = payloadBinding;
        this.body = body;
    }
}
