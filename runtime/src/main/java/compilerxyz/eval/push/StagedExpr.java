package compilerxyz.eval.push;

import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.value.expr.TypeExpr;

public record StagedExpr(Value value) {

  public static Type dynamicType() {
    return new TypeExpr();
  }
}
