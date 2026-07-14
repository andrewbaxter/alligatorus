package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aspect interface for JavaParser-driven extraction by codegen. The Java shape of the
 * interface (method signatures, generic type params, {@code @Nullable}) is the source of truth
 * for what codegen emits; only the identity metadata that cannot be inferred lives here.
 *
 * <p>{@code kind} is the class-name suffix after {@code Aspect} — e.g. for
 * {@code AspectStackValueDynamic}, {@code kind = "StackValueDynamic"}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinAspect {
  long uniqueId();

  String kind();

  String javadoc() default "";
}
