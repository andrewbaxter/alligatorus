package compilerxyz.main.eval.dynamic;

import compilerxyz.prelude.util.Opt;

public final class JavaBytecodeReturn implements JavaBytecode {
    public final Opt<JavaBytecodeReg> value;

    public JavaBytecodeReturn(Opt<JavaBytecodeReg> value) {
        this.value = value;
    }
}
