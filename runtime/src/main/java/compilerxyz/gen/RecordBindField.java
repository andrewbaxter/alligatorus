package compilerxyz.gen;

import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Type;

public record RecordBindField(ConstKey key, TargetBindSub sub) {
  public static Type dynamicType() {
    return GenType.dynamicType();
  }
}
