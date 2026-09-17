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
public sealed interface ValueDynamicSource {
    JavaDataDescriptor valueDynamicSourceDesc();

    boolean valueDynamicSourceIsPlace();

    record Reg(JavaBytecodeReg reg) implements ValueDynamicSource {
        @Override
        public JavaDataDescriptor valueDynamicSourceDesc() {
            return reg.desc;
        }

        @Override
        public boolean valueDynamicSourceIsPlace() {
            return false;
        }
    }

    record Slot(BindingId slot, JavaDataDescriptor desc, BindingSetter setter) implements ValueDynamicSource {
        @Override
        public JavaDataDescriptor valueDynamicSourceDesc() {
            return desc;
        }

        @Override
        public boolean valueDynamicSourceIsPlace() {
            return true;
        }
    }

    record Field(ValueDynamicSource base, JavaInternalName klass, String field, JavaDataDescriptor desc)
            implements ValueDynamicSource {
        @Override
        public JavaDataDescriptor valueDynamicSourceDesc() {
            return desc;
        }

        @Override
        public boolean valueDynamicSourceIsPlace() {
            return base.valueDynamicSourceIsPlace();
        }
    }
}
