package compilerxyz.eval;

import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.code.TypeCodeDynHandleDynamic;
import compilerxyz.eval.value.option.TypeOptionDynamic;

/**
 * Two-variant sum: either {@code None} (no code, just a source location for error reporting)
 * or {@code Target} (target-specific code payload). Mirrors the Rust {@code enum Code { None,
 * Dyn(...) }} shape.
 *
 * <p>Sealed because the two variants are fixed; extensibility to new backend targets happens
 * through the {@link TargetCode} interface that {@code Target} wraps, not through {@code Code}
 * itself.
 */
public sealed interface Code permits Code.None, Code.Target {

  /** Absence of code — carries a loc so errors originating from a "no code" path can report. */
  record None(QExprId loc) implements Code {}

  /** Wrapper around a target-specific code payload. No loc here; the payload carries it. */
  record Target(TargetCode impl) implements Code {}

  static Code none(QExprId loc) {
    return new None(loc);
  }

  /** JVM/dynamic target factory. */
  static Code of(CodeDynamic dynamic) {
    return new Target(dynamic);
  }

  static Type dynamicType() {
    return new TypeOptionDynamic(new TypeCodeDynHandleDynamic());
  }
}
