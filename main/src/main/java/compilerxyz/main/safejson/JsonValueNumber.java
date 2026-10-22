package compilerxyz.main.safejson;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6545L)
public sealed interface JsonValueNumber extends JsonValue permits JsonValueNumberInt, JsonValueNumberFloat {}
