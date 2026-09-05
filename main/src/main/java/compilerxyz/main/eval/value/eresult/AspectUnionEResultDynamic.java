package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.list.TypeListBoxedDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.AspectUnion.Variant;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.prelude.util.TList;

public record AspectUnionEResultDynamic<T extends Type>(TypeEResultDynamic<T> type) implements AspectUnion {

    @Override
    public TList<Variant> aspectUnionVariants() {
        var out = new TList<Variant>();
        out.add(new Variant(new ConstKey.Str("ok"), "ok", type.elementType()));
        out.add(new Variant(new ConstKey.Str("errs"), "errs", new TypeListBoxedDynamic(GenExprErr.dynamicType())));
        return out;
    }
}
