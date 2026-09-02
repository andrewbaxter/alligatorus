package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6340L)
public final class BindResult {
    public final CodeDynamic code;
    public final BindingId id;

    public BindResult(CodeDynamic code, BindingId id) {
        this.code = code;
        this.id = id;
    }
}
