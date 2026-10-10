package compilerxyz.main.meta;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.jvm_object.TypeJvmObjectDynamic;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

public final class Meta {
    public final String kind;
    public final JsonValue data;
    public final TOrderedMap<String, MetaField> fields = new TOrderedMap<>();
    private Opt<Assigned> assigned = Opt.none();

    private record Assigned(Metas owner, MetaId id) {}

    public Meta(String kind, JsonValue data) {
        this.kind = kind;
        this.data = data;
    }

    public static Type dynamicType() {
        return new TypeJvmObjectDynamic();
    }

    public static Meta ofKind(String kind) {
        return new Meta(kind, emptyData());
    }

    public static Meta ofKind(String kind, String desc) {
        return new Meta(kind, descData(desc));
    }

    public static JsonValueObject emptyData() {
        return new JsonValueObject(new TOrderedMap<>());
    }

    public static JsonValueObject descData(String desc) {
        var entries = new TOrderedMap<String, JsonValue>();
        entries.put("desc", new JsonValueString(desc));
        return new JsonValueObject(entries);
    }

    public Meta field(String name, Meta meta) {
        fields.put(name, MetaField.of(meta));
        return this;
    }

    public Meta field(String name, Type type) {
        fields.put(name, MetaField.ofLazy(xx -> xx.vx.ix.metaLog.metaOf(xx, type)));
        return this;
    }

    public Meta field(String name, MetaField field) {
        fields.put(name, field);
        return this;
    }

    public synchronized Opt<MetaId> metaAssignedId(Metas owner) {
        return switch (assigned) {
            case Opt.Some<Assigned> s ->
                s.value().owner() == owner ? Opt.some(s.value().id()) : Opt.none();
            case Opt.None<Assigned> n -> Opt.none();
        };
    }

    public synchronized void metaAssign(Metas owner, MetaId id) {
        assigned = Opt.some(new Assigned(owner, id));
    }
}
