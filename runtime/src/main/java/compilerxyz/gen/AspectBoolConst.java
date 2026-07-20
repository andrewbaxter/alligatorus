package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1006L)
@compilerxyz.annotations.BuiltinTypeId(7204L)
public interface AspectBoolConst extends Aspect {
  boolean aspectBoolConstGet(Value value);
}
