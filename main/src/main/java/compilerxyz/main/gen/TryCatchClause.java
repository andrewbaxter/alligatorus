package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6353L)
public final class TryCatchClause {
    public final Binding binding;
    public final Type excType;
    public final CodeValue body;

    public TryCatchClause(Binding binding, Type excType, CodeValue body) {
        this.binding = binding;
        this.excType = excType;
        this.body = body;
    }
}
