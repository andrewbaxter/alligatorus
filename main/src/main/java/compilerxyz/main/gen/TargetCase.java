package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6351L)
public final class TargetCase {
    public final CodeValue test;
    public final CodeValue body;

    public TargetCase(CodeValue test, CodeValue body) {
        this.test = test;
        this.body = body;
    }
}
