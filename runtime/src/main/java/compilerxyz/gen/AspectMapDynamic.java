package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1012L, identityId = 4012L)
public interface AspectMapDynamic extends Aspect {
  Type aspectMapDynamicKeyType();

  Type aspectMapDynamicValueType();
}
