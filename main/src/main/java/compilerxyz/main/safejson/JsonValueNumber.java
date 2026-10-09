package compilerxyz.main.safejson;

public sealed interface JsonValueNumber extends JsonValue permits JsonValueNumberInt, JsonValueNumberFloat {}
