package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;

public record TryCatchClause(BindingId.Local binding, Type excType, CodeValue body) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
