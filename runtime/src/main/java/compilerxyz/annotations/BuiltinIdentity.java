package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a named identity token (Unique.Builtin) that codegen emits as a static field on
 * GenIdentities. Attach to whichever type/class the identity conceptually belongs to (e.g. the
 * Type/interface it identifies). Codegen scans for these and enforces cross-generator
 * uniqueness of the value against other builtin IDs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinIdentity {
  /** snake_case identity name; will become {@code UPPER_SNAKE_TYPE_ID} on GenIdentities. */
  String name();

  long value();
}
