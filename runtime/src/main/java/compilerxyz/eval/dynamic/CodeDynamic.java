package compilerxyz.eval.dynamic;

import compilerxyz.eval.QExprId;

/**
 * Source-location-annotated wrapper around a {@link JavaBytecode} subtree. Every entry in a
 * {@link JavaBytecodeSequence}'s children is a {@code CodeDynamic}, so the renderer walks the
 * tree emitting a line-number marker at each wrapper. The renderer returns a
 * {@code counter → loc} map so a runtime exception's line number can be mapped back to a source
 * {@link QExprId}.
 *
 * <p>{@code CodeDynamic} does NOT implement {@link JavaBytecode} — it's a location-carrying
 * wrapper, not a variety of bytecode.
 */
public record CodeDynamic(QExprId loc, JavaBytecode code) implements compilerxyz.eval.TargetCode {}
