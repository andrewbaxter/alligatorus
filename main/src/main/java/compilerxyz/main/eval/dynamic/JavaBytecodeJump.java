package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.JumpKey;
import compilerxyz.prelude.util.TList;

public final class JavaBytecodeJump implements JavaBytecode {
    public final JumpKey jumpKey;
    public final TList<JavaBytecodeReg> args;

    public JavaBytecodeJump(JumpKey jumpKey, TList<JavaBytecodeReg> args) {
        this.jumpKey = jumpKey;
        this.args = args;
    }

    public JavaBytecodeJump(JumpKey jumpKey) {
        this(jumpKey, new TList<>());
    }
}
