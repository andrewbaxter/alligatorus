package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1033L,
    kind = "DynamicBoxed",
    javadoc = "Marker aspect implemented by unboxed primitive dynamic types to point to their JVM boxed counterpart.")
public interface AspectDynamicBoxed extends Aspect {
  Type aspectDynamicBoxedBoxedForm();
}
