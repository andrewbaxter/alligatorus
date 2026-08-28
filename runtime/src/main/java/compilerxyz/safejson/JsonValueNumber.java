package compilerxyz.safejson;

public record JsonValueNumber(double value) implements JsonValue {
    public JsonValueNumber(long value) {
        this((double) value);
    }

    public JsonValueNumber(int value) {
        this((double) value);
    }
}
