package compilerxyz.eval.dynamic;

import org.objectweb.asm.tree.AbstractInsnNode;

public class JavaBytecodeInstructionObj implements JavaBytecodeInstruction {

  public final AbstractInsnNode node;

  public JavaBytecodeInstructionObj(AbstractInsnNode node) {
    this.node = node;
  }

  @Override
  public void dispatchMore(MoreDispatcher moreDispatcher) {
    moreDispatcher.handleObj(this);
  }
}
