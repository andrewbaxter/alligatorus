package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1012L, identityId = 4012L)
public interface AspectMapDynamic extends Aspect {
  Type aspectMapDynamicKeyType();

  Type aspectMapDynamicValueType();
}
