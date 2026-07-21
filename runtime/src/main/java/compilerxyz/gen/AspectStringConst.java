package compilerxyz.gen;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1009L)
@BuiltinTypeId(7233L)
public interface AspectStringConst extends Aspect {
  String aspectStringConstGet(Value value);
}
