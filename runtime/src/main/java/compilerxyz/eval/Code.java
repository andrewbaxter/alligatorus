package compilerxyz.eval;

import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.code.TypeCodeDynHandleDynamic;
import compilerxyz.eval.value.option.TypeOptionDynamic;

public sealed interface Code permits Code.None, Code.Target {

  record None(QExprId loc) implements Code {
  }

  record Target(TargetCode impl) implements Code {
  }

  default CodeDynamic dynamic() {
    if (this instanceof Target t && t.impl() instanceof CodeDynamic cd) return cd;
    return null;
  }

  default boolean isNone() {
    return this instanceof None;
  }

  static Code none(QExprId loc) {
    return new None(loc);
  }

  static Code of(CodeDynamic dynamic) {
    return new Target(dynamic);
  }

  static Code of(QExprId loc, CodeDynamic dynamic) {
    return new Target(dynamic);
  }

  static Type dynamicType() {
    return new TypeOptionDynamic(new TypeCodeDynHandleDynamic());
  }
}
