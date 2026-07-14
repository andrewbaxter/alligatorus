package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aspect method as exposed via the enclosing type's {@link SourceInterface} tree
 * registration. The {@code name} is the source-visible method name (may differ from the derived
 * aspect method short-name).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface SourceExposed {
  String name();
}
