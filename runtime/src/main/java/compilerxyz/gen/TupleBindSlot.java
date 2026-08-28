package compilerxyz.gen;

import compilerxyz.eval.Type;

public record TupleBindSlot(TargetBindSub sub) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
