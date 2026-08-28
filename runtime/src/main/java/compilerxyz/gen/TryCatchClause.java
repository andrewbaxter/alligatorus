package compilerxyz.gen;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Type;

public record TryCatchClause(BindingId.Local binding, Type excType, CodeValue body) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
