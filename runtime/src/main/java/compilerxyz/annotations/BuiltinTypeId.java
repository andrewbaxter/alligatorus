package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a runtime Type class with its stable builtin TypeId value. Codegen scans for these and
 * emits the corresponding GenTypeIds.TYPE_&lt;NAME&gt;_TYPE_ID constant. The {@code name} used in
 * GenTypeIds is derived from the annotated class name: {@code TypeListDynamic} becomes
 * {@code TYPE_LIST_DYNAMIC_TYPE_ID} (or if the class name doesn't start with {@code Type}, the
 * class name is snake_case'd and uppercased directly).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinTypeId {
  long value();
}
