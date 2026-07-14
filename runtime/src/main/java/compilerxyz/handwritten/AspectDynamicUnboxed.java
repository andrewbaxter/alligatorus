package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1034L,
    kind = "DynamicUnboxed",
    javadoc = "Marker aspect implemented by JVM boxed dynamic types to point to their unboxed primitive counterpart.")
public interface AspectDynamicUnboxed extends Aspect {
  Type aspectDynamicUnboxedUnboxedForm();
}
