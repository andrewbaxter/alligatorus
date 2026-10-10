package compilerxyz.main.meta;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenAspectField;

public final class MetaSupport {
    private MetaSupport() {}

    public static Meta derive(ExprContext xx, Type type, String kind) {
        var meta = new Meta(kind, Meta.descData(type.typeDesc()));
        fillFields(xx, type, meta);
        return meta;
    }

    public static void fillFields(ExprContext xx, Type type, Meta meta) {
        var aspectOpt = GenAspectField.getMaybe(xx, type);
        if (aspectOpt.optIsNone()) return;
        var aspect = aspectOpt.optUnwrap();
        for (var key : aspect.aspectFieldKeys(xx)) {
            var fieldType = aspect.aspectFieldTypeOpt(xx, key);
            if (fieldType.optIsSome()) {
                meta.field(keyName(key), fieldType.optUnwrap());
            } else {
                meta.fieldRaw(keyName(key), MetaField.dataOnly(Meta.emptyData()));
            }
        }
    }

    public static String keyName(ConstKey key) {
        return switch (key) {
            case ConstKey.Str s -> s.value();
            case ConstKey.Uint u -> Long.toUnsignedString(u.value());
        };
    }
}
