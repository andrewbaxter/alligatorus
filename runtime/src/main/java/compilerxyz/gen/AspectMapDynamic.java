package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1012L, kind = "MapDynamic", javadoc = "Identity aspect for dynamic map")
@compilerxyz.annotations.BuiltinTypeId(7222L)
public interface AspectMapDynamic extends Aspect {
  Type aspectMapDynamicKeyType();

  Type aspectMapDynamicValueType();
}
