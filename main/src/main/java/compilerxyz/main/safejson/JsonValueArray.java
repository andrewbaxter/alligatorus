package compilerxyz.main.safejson;

import compilerxyz.prelude.util.TList;

public record JsonValueArray(TList<JsonValue> elements) implements JsonValue {}
