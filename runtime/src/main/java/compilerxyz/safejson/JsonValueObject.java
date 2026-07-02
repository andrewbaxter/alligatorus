package compilerxyz.safejson;

import compilerxyz.util.TOrderedMap;

public record JsonValueObject(TOrderedMap<String, JsonValue> entries) implements JsonValue {}
