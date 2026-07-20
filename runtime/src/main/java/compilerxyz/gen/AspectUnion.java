package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.TList;

@BuiltinAspect(uniqueId = 1014L, kind = "Union", javadoc = "Identity aspect for union")
@compilerxyz.annotations.BuiltinTypeId(7237L)
public interface AspectUnion extends Aspect {
  record Variant(ConstKey key, String jsTag, Type type) {}

  TList<Variant> aspectUnionVariants();
}
