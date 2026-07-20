package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1001L, kind = "Null", javadoc = "Identity aspect for null")
@compilerxyz.annotations.BuiltinTypeId(7223L)
public interface AspectNull extends Aspect {}
