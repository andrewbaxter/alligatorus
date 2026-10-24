package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.annotations.Immutable;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

@Immutable
@BuiltinFull(uniqueId = 6542L)
@BuiltinPath("types.json_array")
public final class JsonValueArray implements JsonValue {
    private final IList<JsonValue> elements;

    public JsonValueArray(TList<JsonValue> elements) {
        this.elements = IList.from(elements);
    }

    IList<JsonValue> view() {
        return elements;
    }

    public int len() {
        return elements.size();
    }

    public JsonValue get(int index) {
        return elements.get(index);
    }

    public TList<JsonValue> elements() {
        return TList.from(elements);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JsonValueArray o && elements.equals(o.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return JsonValues.serialize(this);
    }
}
