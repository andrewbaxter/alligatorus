package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;

public final class BindResultSlot {
    public final CodeDynamic code;
    public final BindingId id;

    public BindResultSlot(CodeDynamic code, BindingId id) {
        this.code = code;
        this.id = id;
    }
}
