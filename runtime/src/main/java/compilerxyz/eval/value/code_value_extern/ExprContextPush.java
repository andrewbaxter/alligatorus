package compilerxyz.eval.value.code_value_extern;

import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;

public record ExprContextPush(ExprContext context) {

  public static Type dynamicType() {
    return new TypeExprContextPushDynamic();
  }
}
