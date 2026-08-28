package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;

public record TargetBindSub(CodeDynamic data, Type type) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
