package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;

import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;

@BuiltinFull(uniqueId = 6016L)
@BuiltinPath("types.code")
public sealed interface Code permits Code.None, Code.Target {

  record None(QExprId loc) implements Code {
  }

  record Target(TargetCode impl) implements Code {
  }

  default CodeDynamic codeDynamic() {
    return switch (this) {
      case Code.Target t -> t.impl() instanceof CodeDynamic cd ? cd : null;
      case Code.None n -> null;
    };
  }

  default boolean codeIsNone() {
    return switch (this) {
      case Code.None n -> true;
      case Code.Target t -> false;
    };
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

  /**
   * Sequences two pieces of code, preserving the side effects carried by each. Used where a value
   * is produced from operands that may each carry code of their own.
   */
  static Code concat(QExprId loc, Code first, Code second) {
    var firstDyn = first.codeDynamic();
    var secondDyn = second.codeDynamic();
    if (firstDyn == null) return secondDyn == null ? first : second;
    if (secondDyn == null) return first;
    var seq = new JavaBytecodeSequence();
    seq.add(firstDyn);
    seq.add(secondDyn);
    return of(loc, new CodeDynamic(loc, seq));
  }

  static TargetCode targetImpl(Code code) {
    return switch (code) {
      case Code.Target t -> t.impl();
      case Code.None n -> throw new IllegalStateException(
          "Code::targetImpl: code is None, not Target");
    };
  }
}
