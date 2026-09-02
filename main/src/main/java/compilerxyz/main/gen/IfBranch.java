package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6342L)
public final class IfBranch {
    public final CodeValue condition;
    public final CodeValue body;

    public IfBranch(CodeValue condition, CodeValue body) {
        this.condition = condition;
        this.body = body;
    }
}
