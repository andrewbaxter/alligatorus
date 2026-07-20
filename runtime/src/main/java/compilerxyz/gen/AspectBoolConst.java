package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinAspect(uniqueId = 1006L, kind = "BoolConst", javadoc = "Identity aspect for const bool")
@compilerxyz.annotations.BuiltinTypeId(7204L)
public interface AspectBoolConst extends Aspect {
  boolean aspectBoolConstGet(Value value);
}
