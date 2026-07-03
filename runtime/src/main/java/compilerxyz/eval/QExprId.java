package compilerxyz.eval;

import compilerxyz.eval.value.string.TypeStringDynamic;

public record QExprId(SourceId source, long expr) {

  public static final QExprId NONE = new QExprId(new SourceId(0), 0);

  public static Type dynamicType() {
    return new TypeStringDynamic();
  }
}
