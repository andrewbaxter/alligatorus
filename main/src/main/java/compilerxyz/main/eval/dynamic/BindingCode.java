package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.gen.BindingSetter;
import org.objectweb.asm.Opcodes;

public final class BindingCode {
    private BindingCode() {}

    public static CodeDynamic bindingUse(CodeLoc loc, BindingId id) {
        return new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, id));
    }

    public static CodeDynamic bindingUse(QExprId loc, BindingId id) {
        return bindingUse(new CodeLoc.Some(loc), id);
    }

    public static ValueDynamicSource.Slot slotRef(QExprId loc, BindingId id, BindingSetter setter) {
        return slot(loc, id, Opcodes.ALOAD, Opcodes.ASTORE, setter);
    }

    public static ValueDynamicSource.Slot slot(
            QExprId loc, BindingId id, int loadOpcode, int storeOpcode, BindingSetter setter) {
        var codeLoc = new CodeLoc.Some(loc);
        return new ValueDynamicSource.Slot(
                id,
                new CodeDynamic(codeLoc, new JavaBytecodeStoreLoad(loadOpcode, id)),
                new CodeDynamic(codeLoc, new JavaBytecodeStoreLoad(storeOpcode, id)),
                setter);
    }
}
