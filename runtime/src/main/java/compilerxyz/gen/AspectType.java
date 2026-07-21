package compilerxyz.gen;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1010L)
@BuiltinTypeId(7236L)
public interface AspectType extends Aspect {
  Type aspectTypeType(Value value);
}
