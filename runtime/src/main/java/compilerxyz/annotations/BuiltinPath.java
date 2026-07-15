package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinPath {
  /** Dot-separated language path, e.g. "throw", "types.list", "eval.const_eval". */
  String value();

  /** Required for function leaves (attached to static methods); 0 for type leaves. */
  long scopeId() default 0L;
}
