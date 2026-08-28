package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;

public record RecordBindField(ConstKey key, TargetBindSub sub) {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }
}
