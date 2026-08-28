package compilerxyz.main.eval.value;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 1043L)
@BuiltinPath("eval.value_source")
public sealed interface ValueDynamicSource
        permits ValueDynamicSource.Binding, ValueDynamicSource.Expr, ValueDynamicSource.Field {

    CodeDynamic valueDynamicSourceRealize(ExprContext xx, QExprId loc);

    Code valueDynamicSourceDrop(QExprId loc);

    record Binding(BindingId key, Opt<BindingSetter> setter) implements ValueDynamicSource {
        public Binding(BindingId key) {
            this(key, Opt.<BindingSetter>none());
        }

        @Override
        public CodeDynamic valueDynamicSourceRealize(ExprContext xx, QExprId loc) {
            return BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, key);
        }

        @Override
        public Code valueDynamicSourceDrop(QExprId loc) {
            return Code.none(loc);
        }
    }

    record Expr(int stackSize) implements ValueDynamicSource {
        public Expr {
            if (stackSize != 1 && stackSize != 2) {
                throw new IllegalArgumentException(
                        "ValueDynamicSource.Expr stackSize must be 1 or 2, got " + stackSize);
            }
        }

        @Override
        public CodeDynamic valueDynamicSourceRealize(ExprContext xx, QExprId loc) {
            return new CodeDynamic(loc, new JavaBytecodeSequence());
        }

        @Override
        public Code valueDynamicSourceDrop(QExprId loc) {
            int popOp = stackSize == 2 ? Opcodes.POP2 : Opcodes.POP;
            return Code.of(loc, JavaBytecodeUtils.inst(loc, popOp));
        }
    }

    record Field(ValueDynamicSource base, JavaInternalName klass, String slotName, JavaDataDescriptor slotDesc)
            implements ValueDynamicSource {
        @Override
        public CodeDynamic valueDynamicSourceRealize(ExprContext xx, QExprId loc) {
            var seq = new JavaBytecodeSequence();
            seq.add(base.valueDynamicSourceRealize(xx, loc));
            seq.add(JavaBytecodeUtils.accessField(loc, klass, slotName, slotDesc));
            return new CodeDynamic(loc, seq);
        }

        @Override
        public Code valueDynamicSourceDrop(QExprId loc) {
            return base.valueDynamicSourceDrop(loc);
        }
    }
}
