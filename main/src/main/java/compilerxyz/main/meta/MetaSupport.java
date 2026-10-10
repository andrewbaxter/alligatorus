package compilerxyz.main.meta;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class MetaSupport {
    private MetaSupport() {}

    public record Field(String name, MetaField field) {}

    public static Meta derive(ExprContext xx, Type type, String kind) {
        var meta = new Meta(kind, Meta.descData(type.typeDesc()));
        var aspectOpt = GenAspectField.getMaybe(xx, type);
        if (aspectOpt.optIsNone()) return meta;
        var aspect = aspectOpt.optUnwrap();
        for (var key : aspect.aspectFieldKeys(xx)) {
            var fieldType = aspect.aspectFieldTypeOpt(xx, key);
            if (fieldType.optIsSome()) {
                meta.field(keyName(key), fieldType.optUnwrap());
            } else {
                meta.fields.put(keyName(key), new MetaField(Meta.emptyData(), Opt.none()));
            }
        }
        return meta;
    }

    public static TList<Field> fields(Meta meta) {
        var out = new TList<Field>();
        for (var entry : meta.fields.entrySet()) out.add(new Field(entry.getKey(), entry.getValue()));
        return out;
    }

    public static Opt<Meta> follow(ExprContext xx, Meta meta, TList<String> path) {
        var current = meta;
        for (var name : path) {
            var field = current.fields.getOpt(name);
            if (field.optIsNone()) return Opt.none();
            var next = field.optUnwrap().meta();
            if (next.optIsNone()) return Opt.none();
            current = next.optUnwrap().apply(xx);
        }
        return Opt.some(current);
    }

    public static String keyName(ConstKey key) {
        return switch (key) {
            case ConstKey.Str s -> s.value();
            case ConstKey.Uint u -> Long.toUnsignedString(u.value());
        };
    }
}
