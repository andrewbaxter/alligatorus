package compilerxyz.eval.dynamic;

import compilerxyz.eval.QExprId;
import compilerxyz.eval.TargetCode;
import compilerxyz.eval.Type;
import compilerxyz.gen.GenType;

public record CodeDynamic(CodeLoc loc, JavaBytecode code) implements TargetCode, JavaBytecode {
  public CodeDynamic(QExprId qExprId, JavaBytecode code) {
    this(new CodeLoc.Some(qExprId), code);
  }

  public static Type dynamicType() {
    return GenType.dynamicType();
  }

  @Override
  public <T> T javaBytecodeDispatch(JavaBytecode.Dispatcher<T> dispatcher) {
    return code.javaBytecodeDispatch(dispatcher);
  }
}
