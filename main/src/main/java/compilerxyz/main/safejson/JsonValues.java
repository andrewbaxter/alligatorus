package compilerxyz.main.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public final class JsonValues {
    private JsonValues() {}

    public static void encode(JsonWriter writer, JsonValue value) throws IOException {
        switch (value) {
            case JsonValueNull ignored -> writer.nullValue();
            case JsonValueBoolean b -> writer.value(b.value());
            case JsonValueNumberInt n -> writer.value(n.value());
            case JsonValueNumberFloat n -> writer.value(n.value());
            case JsonValueString s -> writer.value(s.value());
            case JsonValueArray a -> {
                writer.beginArray();
                for (var element : a.elements()) {
                    encode(writer, element);
                }
                writer.endArray();
            }
            case JsonValueObject o -> {
                writer.beginObject();
                for (var entry : o.entries().entrySet()) {
                    writer.name(entry.getKey());
                    encode(writer, entry.getValue());
                }
                writer.endObject();
            }
        }
    }

    public static JsonValue parse(String text) throws IOException {
        var reader = new JsonReader(new StringReader(text));
        var value = decode(reader, Json.FieldPath.root());
        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw new IOException("trailing content after the JSON document");
        }
        return value;
    }

    public static JsonValue decode(JsonReader reader, Json.FieldPath path) throws IOException {
        return switch (reader.peek()) {
            case NULL -> {
                reader.nextNull();
                yield new JsonValueNull();
            }
            case BOOLEAN -> new JsonValueBoolean(reader.nextBoolean());
            case NUMBER -> {
                var text = reader.nextString();
                if (text.contains(".") || text.contains("e") || text.contains("E")) {
                    yield new JsonValueNumberFloat(Double.parseDouble(text));
                }
                yield new JsonValueNumberInt(Long.parseLong(text));
            }
            case STRING -> new JsonValueString(reader.nextString());
            case BEGIN_ARRAY -> {
                reader.beginArray();
                var elements = new TList<JsonValue>();
                var index = 0;
                while (reader.hasNext()) {
                    elements.add(decode(reader, path.index(index++)));
                }
                reader.endArray();
                yield new JsonValueArray(elements);
            }
            case BEGIN_OBJECT -> {
                reader.beginObject();
                var entries = new TOrderedMap<String, JsonValue>();
                while (reader.hasNext()) {
                    var key = reader.nextName();
                    entries.put(key, decode(reader, path.field("[\"" + key + "\"]")));
                }
                reader.endObject();
                yield new JsonValueObject(entries);
            }
            default -> throw new Json.DeserializationException("Unexpected token: " + reader.peek(), path);
        };
    }

    public static long numberInt(JsonValue value, Json.FieldPath path) {
        return switch (value) {
            case JsonValueNumberInt n -> n.value();
            default ->
                throw new Json.DeserializationException(
                        "Expected integer, got " + value.getClass().getSimpleName(), path);
        };
    }

    public static String string(JsonValue value, Json.FieldPath path) {
        return switch (value) {
            case JsonValueString s -> s.value();
            default ->
                throw new Json.DeserializationException(
                        "Expected string, got " + value.getClass().getSimpleName(), path);
        };
    }

    public static boolean bool(JsonValue value, Json.FieldPath path) {
        return switch (value) {
            case JsonValueBoolean b -> b.value();
            default ->
                throw new Json.DeserializationException(
                        "Expected boolean, got " + value.getClass().getSimpleName(), path);
        };
    }

    public static TList<JsonValue> array(JsonValue value, Json.FieldPath path) {
        return switch (value) {
            case JsonValueArray a -> a.elements();
            default ->
                throw new Json.DeserializationException(
                        "Expected array, got " + value.getClass().getSimpleName(), path);
        };
    }

    public static TOrderedMap<String, JsonValue> object(JsonValue value, Json.FieldPath path) {
        return switch (value) {
            case JsonValueObject o -> o.entries();
            default ->
                throw new Json.DeserializationException(
                        "Expected object, got " + value.getClass().getSimpleName(), path);
        };
    }

    public static String serialize(JsonValue value) {
        try {
            var writer = new StringWriter();
            var jsonWriter = new JsonWriter(writer);
            jsonWriter.setSerializeNulls(true);
            jsonWriter.setIndent("");
            encode(jsonWriter, value);
            jsonWriter.close();
            return writer.toString();
        } catch (IOException e) {
            throw new AssertionError("In-memory IO failed", e);
        }
    }
}
