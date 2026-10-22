package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6547L)
@BuiltinPath("types.json_int")
public record JsonValueNumberInt(long value) implements JsonValueNumber {}
