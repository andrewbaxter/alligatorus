package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aspect/trait method as exposed via the enclosing type's SourceInterface tree
 * registration. The source-visible name is derived from the Java method name: strip the
 * lowercase-first-char interface-name prefix, lowercase the first remaining char.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface SourceExposed {}
