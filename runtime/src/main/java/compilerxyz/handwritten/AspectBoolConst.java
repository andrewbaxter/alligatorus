package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinAspect(uniqueId = 1006L, kind = "BoolConst", javadoc = "Identity aspect for const bool")
public interface AspectBoolConst extends Aspect {
  boolean aspectBoolConstGet(Value value);
}
