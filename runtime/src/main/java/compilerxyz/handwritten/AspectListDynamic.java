package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1011L, kind = "ListDynamic", javadoc = "Identity aspect for dynamic list")
public interface AspectListDynamic extends Aspect {}
