package compilerxyz.safejson;

public sealed interface JsonValue
        permits JsonValueNull, JsonValueBoolean, JsonValueNumber, JsonValueString, JsonValueArray, JsonValueObject {}
