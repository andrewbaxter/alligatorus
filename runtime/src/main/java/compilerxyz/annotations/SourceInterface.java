package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aspect interface as ALSO being exposed as a source-visible interface in the language's
 * builtin scope tree. Only methods explicitly annotated with {@link SourceExposed} are visible;
 * unmarked methods stay runtime-only.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SourceInterface {
  long uniqueId();

  int scopeId();
}
