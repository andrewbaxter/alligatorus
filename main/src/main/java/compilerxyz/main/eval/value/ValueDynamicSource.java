package compilerxyz.main.eval.value;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1043L)
@BuiltinPath("eval.value_source")
public sealed interface ValueDynamicSource permits ValueDynamicSource.Slot, ValueDynamicSourceJvm {
    @BuiltinFull(uniqueId = 6521L)
    record Slot(BindingId slot, BindingSetter setter) implements ValueDynamicSource {}

    record Reg(JavaBytecodeReg reg) implements ValueDynamicSourceJvm {}

    record Field(ValueDynamicSource base, JavaInternalName klass, String field, JavaDataDescriptor desc)
            implements ValueDynamicSourceJvm {}
}
