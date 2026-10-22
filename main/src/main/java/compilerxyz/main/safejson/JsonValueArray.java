package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6542L)
@BuiltinPath("types.json_array")
public record JsonValueArray(TList<JsonValue> elements) implements JsonValue {}
