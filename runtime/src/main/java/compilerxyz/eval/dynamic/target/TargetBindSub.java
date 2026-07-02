package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.any.TypeAnyDynamic;

public record TargetBindSub(CodeDynamic data, Type type) {
    public static Type dynamicType() {
        return new TypeAnyDynamic();
    }
}
