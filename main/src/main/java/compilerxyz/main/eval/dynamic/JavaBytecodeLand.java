package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.JumpKey;
import compilerxyz.prelude.util.TList;

public final class JavaBytecodeLand implements JavaBytecode {
    public final JumpKey jumpKey;
    public final TList<JavaBytecodeReg> params;

    public JavaBytecodeLand(JumpKey jumpKey, TList<JavaBytecodeReg> params) {
        this.jumpKey = jumpKey;
        this.params = params;
    }

    public JavaBytecodeLand(JumpKey jumpKey) {
        this(jumpKey, new TList<>());
    }
}
