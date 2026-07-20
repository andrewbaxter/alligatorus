package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;

public record TargetBindSub(CodeDynamic data, Type type) {
    public static Type dynamicType() {
        return compilerxyz.gen.GenType.dynamicType();
    }
}
