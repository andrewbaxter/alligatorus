package compilerxyz.gen;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Type;

public record IfBranch(CodeValue condition, CodeValue body) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
