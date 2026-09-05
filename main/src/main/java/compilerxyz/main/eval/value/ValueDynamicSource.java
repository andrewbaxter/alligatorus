package compilerxyz.main.eval.value;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 1043L)
@BuiltinPath("eval.value_source")
public sealed interface ValueDynamicSource
        permits ValueDynamicSource.Stack, ValueDynamicSource.Slot, ValueDynamicSource.Field {

    CodeDynamic valueDynamicSourceRealize(QExprId loc);

    EResult<CodeDynamic> valueDynamicSourceStore(QExprId loc, CodeDynamic value);

    Code valueDynamicSourceDrop(QExprId loc);

    boolean valueDynamicSourceIsStackRooted();

    record Stack(int stackSize) implements ValueDynamicSource {
        public Stack {
            if (stackSize != 1 && stackSize != 2) {
                throw new IllegalArgumentException(
                        "ValueDynamicSource.Stack stackSize must be 1 or 2, got " + stackSize);
            }
        }

        @Override
        public CodeDynamic valueDynamicSourceRealize(QExprId loc) {
            return new CodeDynamic(loc, new JavaBytecodeSequence());
        }

        @Override
        public EResult<CodeDynamic> valueDynamicSourceStore(QExprId loc, CodeDynamic value) {
            return EResult.err(loc, "Cannot modify a temporary value");
        }

        @Override
        public Code valueDynamicSourceDrop(QExprId loc) {
            int popOp = stackSize == 2 ? Opcodes.POP2 : Opcodes.POP;
            return Code.target(JavaBytecodeUtils.inst(loc, popOp));
        }

        @Override
        public boolean valueDynamicSourceIsStackRooted() {
            return true;
        }
    }

    record Slot(BindingId slot, CodeDynamic load, CodeDynamic store, BindingSetter setter)
            implements ValueDynamicSource {

        @Override
        public CodeDynamic valueDynamicSourceRealize(QExprId loc) {
            return load;
        }

        @Override
        public EResult<CodeDynamic> valueDynamicSourceStore(QExprId loc, CodeDynamic value) {
            var seq = new JavaBytecodeSequence();
            seq.add(value);
            seq.add(store);
            return EResult.ok(new CodeDynamic(loc, seq));
        }

        @Override
        public Code valueDynamicSourceDrop(QExprId loc) {
            return Code.none(loc);
        }

        @Override
        public boolean valueDynamicSourceIsStackRooted() {
            return false;
        }
    }

    record Field(ValueDynamicSource base, JavaInternalName klass, String slotName, JavaDataDescriptor slotDesc)
            implements ValueDynamicSource {

        @Override
        public CodeDynamic valueDynamicSourceRealize(QExprId loc) {
            var seq = new JavaBytecodeSequence();
            seq.add(base.valueDynamicSourceRealize(loc));
            seq.add(JavaBytecodeUtils.accessField(loc, klass, slotName, slotDesc));
            return new CodeDynamic(loc, seq);
        }

        @Override
        public EResult<CodeDynamic> valueDynamicSourceStore(QExprId loc, CodeDynamic value) {
            if (base.valueDynamicSourceIsStackRooted()) {
                return EResult.err(loc, "Cannot modify a temporary value");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(base.valueDynamicSourceRealize(loc));
            seq.add(value);
            seq.add(JavaBytecodeUtils.setField(new CodeLoc.Some(loc), klass, slotName, slotDesc));
            return EResult.ok(new CodeDynamic(loc, seq));
        }

        @Override
        public Code valueDynamicSourceDrop(QExprId loc) {
            return base.valueDynamicSourceDrop(loc);
        }

        @Override
        public boolean valueDynamicSourceIsStackRooted() {
            return base.valueDynamicSourceIsStackRooted();
        }
    }
}
