package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(uniqueId = 1012L, kind = "MapDynamic", javadoc = "Identity aspect for dynamic map")
public interface AspectMapDynamic extends Aspect {
  Type aspectMapDynamicKeyType();

  Type aspectMapDynamicValueType();
}
