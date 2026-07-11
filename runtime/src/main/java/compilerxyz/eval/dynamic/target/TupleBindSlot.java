package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.Type;

public record TupleBindSlot(TargetBindSub sub) {
  public static Type dynamicType() {
    return Type.dynamicType();
  }
}
