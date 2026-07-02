package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.any.TypeAnyDynamic;

/**
 * Port of {@code eval::dynamic::target::TargetBindSub}. Carries the dynamic-side code and its
 * runtime {@link Type} so a {@link compilerxyz.eval.dynamic.Target} can bind it to a fresh local.
 */
public record TargetBindSub(CodeDynamic data, Type type) {
  /**
   * Port of {@code impl Push for TargetBindSub::dynamic_type} ({@code eval/push.rs:416}).
   * Returns {@link TypeAnyDynamic} — the sub is opaque at the dynamic boundary.
   */
  public static Type dynamicType() {
    return new TypeAnyDynamic();
  }
}
