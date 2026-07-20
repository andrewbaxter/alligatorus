package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1012L)
@compilerxyz.annotations.BuiltinTypeId(7222L)
public interface AspectMapDynamic extends Aspect {
  Type aspectMapDynamicKeyType();

  Type aspectMapDynamicValueType();
}
