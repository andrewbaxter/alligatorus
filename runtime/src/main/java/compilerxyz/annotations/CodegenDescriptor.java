package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks a static helper method whose JVM reference the compiler emits into generated target
 * bytecode. Codegen generates a typed {@code JavaStaticMethodRef} constant for it (in
 * {@code compilerxyz.gen.GenCodegenDescriptors}), so emit sites reference the method by a real
 * constant instead of a string name. Reachability analysis treats annotated methods as roots.
 */
@Target(ElementType.METHOD)
public @interface CodegenDescriptor {}
