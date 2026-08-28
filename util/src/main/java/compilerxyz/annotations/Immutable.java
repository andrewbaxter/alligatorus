package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Asserts that the annotated type is deeply immutable: every instance field is final and of an
 * immutable type (primitives, whitelisted leaf types, immutable collections of immutable types, or
 * other immutable types; cycles count as immutable). The {@code ImmutableAnnotationValid} lint
 * verifies the assertion structurally — the annotation is never trusted, only checked.
 */
@Target(ElementType.TYPE)
public @interface Immutable {}
