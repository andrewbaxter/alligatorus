package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6546L)
@BuiltinPath("types.json_float")
public record JsonValueNumberFloat(double value) implements JsonValueNumber {}
