package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks a field-less final class whose instances function as globally unique keys: reference
 * identity is the type's defined equality. The {@code IdentityTokenValid} lint verifies the shape
 * (final class, no instance fields — a record would make all instances equal), and
 * {@code NoReferenceEquality} exempts comparisons whose operands are annotated types.
 */
@Target(ElementType.TYPE)
public @interface IdentityToken {}
