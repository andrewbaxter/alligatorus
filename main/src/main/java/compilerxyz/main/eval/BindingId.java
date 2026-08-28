package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.main.gen.GenType;

public sealed interface BindingId {
    static Type dynamicType() {
        return GenType.dynamicType();
    }

    record Local(JavaBytecodeBindingKey key) implements BindingId {}

    static Local localFresh() {
        return new Local(new JavaBytecodeBindingKey());
    }
}
