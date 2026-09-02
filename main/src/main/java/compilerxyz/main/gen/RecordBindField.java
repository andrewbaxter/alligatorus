package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6343L)
public final class RecordBindField {
    public final ConstKey key;
    public final TargetBindSub sub;

    public RecordBindField(ConstKey key, TargetBindSub sub) {
        this.key = key;
        this.sub = sub;
    }
}
