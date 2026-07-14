package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1003L, kind = "NumberDynamic", javadoc = "Identity aspect for dynamic number")
public interface AspectNumberDynamic extends Aspect {}
