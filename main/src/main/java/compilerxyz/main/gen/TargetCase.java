package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6351L)
public final class TargetCase {
    public final String dynamicTag;
    public final Type variantType;
    public final int variantIndex;
    public final BindingId payloadBinding;
    public final CodeValue body;

    public TargetCase(String dynamicTag, Type variantType, int variantIndex, BindingId payloadBinding, CodeValue body) {
        this.dynamicTag = dynamicTag;
        this.variantType = variantType;
        this.variantIndex = variantIndex;
        this.payloadBinding = payloadBinding;
        this.body = body;
    }
}
