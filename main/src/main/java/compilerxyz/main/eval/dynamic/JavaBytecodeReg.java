package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6450L)
public final class JavaBytecodeReg {
    public final BindingId key = new BindingId();
    public final JavaDataDescriptor desc;

    public JavaBytecodeReg(JavaDataDescriptor desc) {
        this.desc = desc;
    }
}
