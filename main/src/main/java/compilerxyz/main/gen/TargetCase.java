package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;

public record TargetCase(
        String dynamicTag, Type variantType, int variantIndex, BindingId.Local payloadBinding, CodeValue body) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
