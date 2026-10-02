package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.EvalContext;
import org.objectweb.asm.tree.AbstractInsnNode;

public sealed interface JavaBytecodeInsnBody {
    record Op(int opcode) implements JavaBytecodeInsnBody {}

    record Node(AbstractInsnNode node) implements JavaBytecodeInsnBody {}

    record Var(int opcode, BindingId slot) implements JavaBytecodeInsnBody {}

    record VarIndexed(int opcode, int index) implements JavaBytecodeInsnBody {}

    record New(JavaInternalName klass, JavaMethodDescriptor ctor) implements JavaBytecodeInsnBody {}

    record ArrayNew(JavaDataDescriptor element) implements JavaBytecodeInsnBody {}

    record External(EvalContext.ExternalEntry entry) implements JavaBytecodeInsnBody {}
}
