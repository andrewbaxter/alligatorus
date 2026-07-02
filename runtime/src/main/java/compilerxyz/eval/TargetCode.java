package compilerxyz.eval;

/**
 * Marker interface for a target-specific code payload wrapped inside a {@link Code.Target}
 * variant. The JVM/dynamic target implementation is {@link compilerxyz.eval.dynamic.CodeDynamic};
 * other targets add their own implementations here.
 */
public interface TargetCode {}
