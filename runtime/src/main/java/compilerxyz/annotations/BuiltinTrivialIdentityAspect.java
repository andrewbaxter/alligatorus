package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a named identity token (Unique.Builtin) that codegen emits as a static field on
 * GenTrivialIdentityAspects. Attach to whichever type/class the identity conceptually belongs to (e.g. the
 * Type/interface it identifies). The generated constant name is derived from the owning class
 * name as {@code UPPER_SNAKE(className) + "_IDENTITY"}. Codegen scans for these and enforces
 * cross-generator uniqueness of the value against other builtin IDs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinTrivialIdentityAspect {
  long value();
}
