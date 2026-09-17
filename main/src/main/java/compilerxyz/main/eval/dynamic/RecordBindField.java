package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ConstKey;

public final class RecordBindField {
    public final ConstKey key;
    public final BindSubDynamic sub;

    public RecordBindField(ConstKey key, BindSubDynamic sub) {
        this.key = key;
        this.sub = sub;
    }
}
