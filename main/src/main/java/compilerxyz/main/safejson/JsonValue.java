package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6419L)
public sealed interface JsonValue
        permits JsonValueNull, JsonValueBoolean, JsonValueNumber, JsonValueString, JsonValueArray, JsonValueObject {}
