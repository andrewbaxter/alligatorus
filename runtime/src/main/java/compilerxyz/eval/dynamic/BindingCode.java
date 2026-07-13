package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.QExprId;
import org.objectweb.asm.Opcodes;

public final class BindingCode {
  private BindingCode() {}

  public static CodeDynamic bindingUse(CodeLoc loc, EvalContext vx, BindingId id) {
    return switch (id) {
      case BindingId.Local local ->
          new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, local.key()));
    };
  }

  public static CodeDynamic bindingUse(QExprId loc, EvalContext vx, BindingId id) {
    return bindingUse(new CodeLoc.Some(loc), vx, id);
  }

  public static CodeDynamic bindingSet(
      CodeLoc loc, EvalContext vx, BindingId id, CodeDynamic value) {
    var seq = new JavaBytecodeSequence();
    seq.add(value);
    switch (id) {
      case BindingId.Local local ->
          seq.add(
              new CodeDynamic(
                  loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, local.key())));
    }
    return new CodeDynamic(loc, seq);
  }

  public static CodeDynamic bindingSet(
      QExprId loc, EvalContext vx, BindingId id, CodeDynamic value) {
    return bindingSet(new CodeLoc.Some(loc), vx, id, value);
  }
}
