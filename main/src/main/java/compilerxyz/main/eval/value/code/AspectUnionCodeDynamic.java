package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectUnionCodeDynamic implements AspectUnion {
    public static final AspectUnionCodeDynamic INSTANCE = new AspectUnionCodeDynamic();

    private AspectUnionCodeDynamic() {}

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        out.add(new AspectUnion.Variant(new ConstKey.Str("none"), "none", new Tuple(Opt.none(), IList.of())));
        out.add(new AspectUnion.Variant(new ConstKey.Str("target"), "target", new TypeCodeTargetDynamic()));
        return out;
    }
}
