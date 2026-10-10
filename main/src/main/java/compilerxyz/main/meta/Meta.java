package compilerxyz.main.meta;

import compilerxyz.main.eval.Type;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

@BuiltinFull(uniqueId = 6473L)
@BuiltinPath("eval.meta")
public final class Meta {
    public final String kind;
    public final JsonValue data;
    final TOrderedMap<String, MetaField> fields = new TOrderedMap<>();

    public Meta(String kind, JsonValue data) {
        this.kind = kind;
        this.data = data;
    }

    public static Meta ofKind(String kind) {
        return new Meta(kind, emptyData());
    }

    public static JsonValueObject emptyData() {
        return new JsonValueObject(new TOrderedMap<>());
    }

    public static JsonValueObject descData(String desc) {
        var entries = new TOrderedMap<String, JsonValue>();
        entries.put("desc", new JsonValueString(desc));
        return new JsonValueObject(entries);
    }

    public Meta field(String name, Type type) {
        fields.put(name, new MetaField(emptyData(), Opt.some(xx -> type.typeMeta(xx))));
        return this;
    }

    public Meta fieldMeta(String name, Meta meta) {
        fields.put(name, new MetaField(emptyData(), Opt.some(xx -> meta)));
        return this;
    }
}
