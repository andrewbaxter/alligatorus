package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;

public record BindResult(CodeDynamic code, BindingId.Local id) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
