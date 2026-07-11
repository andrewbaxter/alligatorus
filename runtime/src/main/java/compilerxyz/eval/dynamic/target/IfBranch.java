package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Expr;
import compilerxyz.eval.Type;

public record IfBranch(CodeValue condition, Expr body) {
  public static Type dynamicType() {
    return Type.dynamicType();
  }
}
