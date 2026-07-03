package compilerxyz.eval.dynamic;

import compilerxyz.eval.QExprId;
import compilerxyz.eval.TargetCode;

public record CodeDynamic(CodeLoc loc, JavaBytecode code) implements TargetCode, JavaBytecode {
  public CodeDynamic(QExprId qExprId, JavaBytecode code) {
    this(new CodeLoc.Some(qExprId), code);
  }

  @Override
  public <T> T dispatch(JavaBytecode.Dispatcher<T> dispatcher) {
    return code.dispatch(dispatcher);
  }
}
