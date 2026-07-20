package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1009L)
@compilerxyz.annotations.BuiltinTypeId(7233L)
public interface AspectStringConst extends Aspect {
  String aspectStringConstGet(Value value);
}
