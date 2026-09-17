package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.JumpKey;

public final class JavaBytecodeGuarded implements JavaBytecode {
    public final JumpKey dest;
    public final CodeDynamic inner;

    public JavaBytecodeGuarded(JumpKey dest, CodeDynamic inner) {
        this.dest = dest;
        this.inner = inner;
    }
}
