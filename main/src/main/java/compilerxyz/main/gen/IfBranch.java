package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;

public record IfBranch(CodeValue condition, CodeValue body) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
