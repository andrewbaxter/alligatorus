package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.JumpKey;
import compilerxyz.prelude.util.TList;

public final class JavaBytecodeBranch implements JavaBytecode {
    public final int opcode;
    public final TList<JavaBytecodeReg> args;
    public final JumpKey jumpKey;

    public JavaBytecodeBranch(int opcode, TList<JavaBytecodeReg> args, JumpKey jumpKey) {
        this.opcode = opcode;
        this.args = args;
        this.jumpKey = jumpKey;
    }
}
