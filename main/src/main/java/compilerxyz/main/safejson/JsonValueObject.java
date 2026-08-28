package compilerxyz.main.safejson;

import compilerxyz.prelude.util.TOrderedMap;

public record JsonValueObject(TOrderedMap<String, JsonValue> entries) implements JsonValue {}
