package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@Builtin(uniqueId = 1008L)
@BuiltinTypeId(7224L)
@BuiltinPath("eval.aspect_int")
public interface AspectInt extends Aspect {
  long aspectIntGet(Value value);
}
