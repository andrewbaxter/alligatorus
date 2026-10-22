package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.TOrderedMap;

@BuiltinFull(uniqueId = 6548L)
@BuiltinPath("types.json_object")
public record JsonValueObject(TOrderedMap<String, JsonValue> entries) implements JsonValue {}
