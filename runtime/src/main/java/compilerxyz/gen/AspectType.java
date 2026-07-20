package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinAspect(uniqueId = 1010L, kind = "Type", javadoc = "Converts a known type Value into a type rust value")
@compilerxyz.annotations.BuiltinTypeId(7236L)
public interface AspectType extends Aspect {
  Type aspectTypeType(Value value);
}
