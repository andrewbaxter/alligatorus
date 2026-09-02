package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.QExprId;
import org.objectweb.asm.Opcodes;

public final class BindingCode {
    private BindingCode() {}

    public static CodeDynamic bindingUse(CodeLoc loc, BindingId id) {
        return new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, id));
    }

    public static CodeDynamic bindingUse(QExprId loc, BindingId id) {
        return bindingUse(new CodeLoc.Some(loc), id);
    }
}
