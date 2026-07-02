package compilerxyz.eval;

import compilerxyz.eval.value.string.TypeStringDynamic;

public record QExprId(SourceId source, long expr) {

  public static Type dynamicType() {
    return new TypeStringDynamic();
  }
}
