package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface for codegen scanning as a builtin aspect or trait. Whether it's an aspect
 * (registered as a source-visible Aspect) or a trait (a runtime value/binding/... interface with
 * a Gen&lt;X&gt; companion) is detected structurally: interfaces that extend {@code Aspect} are
 * aspects, everything else is a trait. Traits carry a non-zero {@code scopeId}; aspects leave it
 * at zero.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinFull {
  long uniqueId();

  int scopeId() default 0;

  /**
   * Identity id for aspect interfaces: answers "is this type this interface" (used by casts),
   * distinct from {@code uniqueId} which answers "does this type have this aspect" (used by
   * capability lookups). Required (non-zero) for aspects; unused for traits.
   */
  long identityId() default 0;
}
