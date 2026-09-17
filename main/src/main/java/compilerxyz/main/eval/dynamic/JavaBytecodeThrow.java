package compilerxyz.main.eval.dynamic;

public final class JavaBytecodeThrow implements JavaBytecode {
    public final JavaBytecodeReg exception;

    public JavaBytecodeThrow(JavaBytecodeReg exception) {
        this.exception = exception;
    }
}
