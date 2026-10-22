package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6543L)
@BuiltinPath("types.json_bool")
public record JsonValueBoolean(boolean value) implements JsonValue {}
