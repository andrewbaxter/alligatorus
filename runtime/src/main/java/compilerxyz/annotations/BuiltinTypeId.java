package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a runtime Type class with its stable builtin TypeId. Codegen scans for these and emits
 * the corresponding GenTypeIds.&lt;NAME&gt;_TYPE_ID constant. The constant name is always derived
 * from the class name (snake_case, uppercased, with {@code _TYPE_ID} suffix).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinTypeId {
  long value();
}
