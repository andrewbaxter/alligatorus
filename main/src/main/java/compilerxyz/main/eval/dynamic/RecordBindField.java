package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.gen.TargetBindSub;

public final class RecordBindField {
    public final ConstKey key;
    public final TargetBindSub sub;

    public RecordBindField(ConstKey key, TargetBindSub sub) {
        this.key = key;
        this.sub = sub;
    }
}
