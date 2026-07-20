package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1010L)
@compilerxyz.annotations.BuiltinTypeId(7236L)
public interface AspectType extends Aspect {
  Type aspectTypeType(Value value);
}
