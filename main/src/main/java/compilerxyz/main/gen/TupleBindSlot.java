package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;

public record TupleBindSlot(TargetBindSub sub) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
