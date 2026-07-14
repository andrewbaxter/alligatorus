package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1001L, kind = "Null", javadoc = "Identity aspect for null")
public interface AspectNull extends Aspect {}
