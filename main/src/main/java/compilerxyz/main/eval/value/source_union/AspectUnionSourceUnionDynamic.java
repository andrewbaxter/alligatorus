package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.gen.AspectUnion;
import compilerxyz.prelude.util.TList;

public record AspectUnionSourceUnionDynamic(TypeSourceUnionDynamic type) implements AspectUnion {

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        for (var v : type.variants) {
            out.add(new AspectUnion.Variant(v.tag, v.jsTag, v.type));
        }
        return out;
    }
}
