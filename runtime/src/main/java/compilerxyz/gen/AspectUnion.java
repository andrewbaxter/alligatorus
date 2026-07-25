package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.TList;

@Builtin(uniqueId = 1014L)
public interface AspectUnion extends Aspect {
  record Variant(ConstKey key, String jsTag, Type type) {}

  TList<Variant> aspectUnionVariants();
}
