package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a runtime trait interface (Value, Binding, Target, CodeDyn, ...) that is registered in
 * the language's builtin scope tree. Companion to {@link SourceExposed} on individual methods —
 * only methods carrying that annotation appear in the source-visible interface.
 *
 * <p>The trait's short name (used to name the scope-tree entry) is derived by
 * snake_case'ing the enclosing interface's simple name — e.g. {@code Value} -> {@code value},
 * {@code CodeDyn} -> {@code code_dyn}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinTrait {
  long uniqueId();

  int scopeId();

  String fieldPrefix();

  /** GenTypeIds constant name used for the trait's runtime Type descriptor. */
  String typeIdConstant();

  /** Dot-separated language path in the builtin scope tree; empty means not placed in tree. */
  String path() default "";
}
