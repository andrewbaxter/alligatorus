package compilerxyz.eval.push;

import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.value.expr.TypeExpr;

public record StagedExpr(Value value) {

  public static Type dynamicType() {
    return new TypeExpr();
  }
}
