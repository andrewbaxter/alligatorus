package compilerxyz.eval.binding;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.gen.Binding;
import org.objectweb.asm.Opcodes;

/**
 * Synthetic scope entry for a foreach iterator that lives on the JVM operand
 * stack across body iterations. It is never resolved by name; its sole role
 * is to contribute a {@code POP} to scope-exit cleanup so that a Return-Jump
 * escaping the loop body restores stack shape.
 */
public final class BindingIteratorStack implements Binding {
  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    throw new IllegalStateException(
        "BindingIteratorStack: iterator is a stack-only scope entry, never resolved by name");
  }

  @Override
  public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
    return EResult.ok(Code.of(JavaBytecodeUtils.inst(loc, Opcodes.POP)));
  }
}
