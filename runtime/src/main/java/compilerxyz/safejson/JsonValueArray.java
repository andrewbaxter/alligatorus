package compilerxyz.safejson;

import compilerxyz.util.TList;

public record JsonValueArray(TList<JsonValue> elements) implements JsonValue {}
