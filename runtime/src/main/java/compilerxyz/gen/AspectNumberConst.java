package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinAspect(uniqueId = 1008L, kind = "NumberConst", javadoc = "Identity aspect for const number")
@compilerxyz.annotations.BuiltinTypeId(7224L)
public interface AspectNumberConst extends Aspect {
  double aspectNumberConstGet(Value value);
}
