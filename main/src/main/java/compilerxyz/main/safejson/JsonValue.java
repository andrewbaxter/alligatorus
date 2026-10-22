package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6419L)
@BuiltinPath("types.json_value")
public sealed interface JsonValue
        permits JsonValueNull, JsonValueBoolean, JsonValueNumber, JsonValueString, JsonValueArray, JsonValueObject {}
