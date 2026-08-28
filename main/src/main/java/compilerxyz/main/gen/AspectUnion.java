package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 1014L, identityId = 4014L)
public interface AspectUnion extends Aspect {
    record Variant(ConstKey key, String jsTag, Type type) {}

    TList<Variant> aspectUnionVariants();
}
