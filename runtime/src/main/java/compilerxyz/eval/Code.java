package compilerxyz.eval;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;

import compilerxyz.eval.dynamic.CodeDynamic;

@Builtin(uniqueId = 6016L)
@BuiltinPath("types.code")
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

  static Code target(TargetCode impl) {
    return new Target(impl);
  }

  static TargetCode targetImpl(Code code) {
    if (!(code instanceof Target t)) {
      throw new IllegalStateException(
          "Code::targetImpl: code is " + code.getClass().getSimpleName() + ", not Target");
    }
    return t.impl();
  }
}
