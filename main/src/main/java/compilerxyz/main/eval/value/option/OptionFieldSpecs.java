package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TMap;

public final class OptionFieldSpecs {
    private OptionFieldSpecs() {}

    public static TMap<ConstKey, Type> factories(Type elementType) {
        return TMap.ofEntries(
                new Pair<>(new ConstKey.Str("some"), new TypeOptionVariantConstructorDynamic(elementType, true)),
                new Pair<>(new ConstKey.Str("none"), new TypeOptionVariantConstructorDynamic(elementType, false)));
    }
}
