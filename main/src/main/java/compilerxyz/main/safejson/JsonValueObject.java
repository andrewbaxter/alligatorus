package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.annotations.Immutable;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

@Immutable
@BuiltinFull(uniqueId = 6548L)
@BuiltinPath("types.json_object")
public final class JsonValueObject implements JsonValue {
    private final IOrderedMap<String, JsonValue> entries;

    public JsonValueObject(TOrderedMap<String, JsonValue> entries) {
        this.entries = IOrderedMap.from(entries);
    }

    IOrderedMap<String, JsonValue> view() {
        return entries;
    }

    public int len() {
        return entries.size();
    }

    public Opt<JsonValue> get(String key) {
        return entries.getOpt(key);
    }

    public TList<String> keys() {
        return entries.keys();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JsonValueObject o) || o.entries.size() != entries.size()) return false;
        for (var entry : entries) {
            var theirs = o.entries.getOpt(entry.getKey());
            if (theirs.optIsNone() || !theirs.optUnwrap().equals(entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        var hash = 0;
        for (var entry : entries)
            hash += entry.getKey().hashCode() ^ entry.getValue().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return JsonValues.serialize(this);
    }
}
