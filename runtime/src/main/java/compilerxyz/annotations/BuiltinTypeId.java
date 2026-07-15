package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a runtime Type class with its stable builtin TypeId. Codegen scans for these and emits
 * the corresponding GenTypeIds.TYPE_&lt;NAME&gt;_TYPE_ID constant. By default the name is derived
 * from the class name (snake_case); use {@link #name} to override when the derived name doesn't
 * match the historical GenTypeIds constant.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinTypeId {
  long value();

  /** Snake_case identity name. Empty = derive from class name. */
  String name() default "";
}
