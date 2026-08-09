package compilerxyz.gen;

import compilerxyz.eval.Type;

public record TupleBindSlot(TargetBindSub sub) {
  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }
}
