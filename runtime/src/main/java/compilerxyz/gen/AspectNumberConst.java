package compilerxyz.gen;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1008L)
@BuiltinTypeId(7224L)
public interface AspectNumberConst extends Aspect {
  double aspectNumberConstGet(Value value);
}
