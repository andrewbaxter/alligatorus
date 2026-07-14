package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1005L, kind = "StringDynamic", javadoc = "Identity aspect for dynamic string")
public interface AspectStringDynamic extends Aspect {}
