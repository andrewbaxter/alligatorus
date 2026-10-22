package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6549L)
@BuiltinPath("types.json_string")
public record JsonValueString(String value) implements JsonValue {}
