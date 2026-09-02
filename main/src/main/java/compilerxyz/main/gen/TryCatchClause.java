package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6353L)
public final class TryCatchClause {
    public final BindingId binding;
    public final Type excType;
    public final CodeValue body;

    public TryCatchClause(BindingId binding, Type excType, CodeValue body) {
        this.binding = binding;
        this.excType = excType;
        this.body = body;
    }
}
