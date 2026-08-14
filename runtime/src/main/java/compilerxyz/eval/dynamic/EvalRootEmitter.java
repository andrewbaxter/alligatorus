package compilerxyz.eval.dynamic;

import compilerxyz.eval.ExprContext;
import compilerxyz.util.TList;

public final class EvalRootEmitter {
  public static final String EXPR_CONTEXT_FIELD = "__exprContext";

  public static final JavaDataDescriptor DESC_EXPR_CONTEXT =
      JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(ExprContext.class));

  public static String toInternalName(String binaryName) {
    return binaryName.replace('.', '/');
  }

  private EvalRootEmitter() {}
}
