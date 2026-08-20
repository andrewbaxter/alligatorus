package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.TreeMap;

public final class JsonValues {
  private JsonValues() {}

  public static void encode(JsonWriter writer, JsonValue value, Json.Style style)
      throws IOException {
    switch (value) {
      case JsonValueNull ignored -> writer.nullValue();
      case JsonValueBoolean b -> writer.value(b.value());
      case JsonValueNumber n -> Json.serializeDouble(writer, n.value(), style);
      case JsonValueString s -> writer.value(s.value());
      case JsonValueArray a -> {
        writer.beginArray();
        for (var element : a.elements()) {
          encode(writer, element, style);
        }
        writer.endArray();
      }
      case JsonValueObject o -> {
        writer.beginObject();
        var entries = o.entries();
        if (style == Json.Style.CANONICAL) {
          var sorted = new TreeMap<String, JsonValue>(Comparator.naturalOrder());
          sorted.putAll(entries);
          for (var entry : sorted.entrySet()) {
            writer.name(entry.getKey());
            encode(writer, entry.getValue(), style);
          }
        } else {
          for (var entry : entries.entrySet()) {
            writer.name(entry.getKey());
            encode(writer, entry.getValue(), style);
          }
        }
        writer.endObject();
      }
    }
  }

  public static JsonValue decode(JsonReader reader, Json.FieldPath path) throws IOException {
    return switch (reader.peek()) {
      case NULL -> {
        reader.nextNull();
        yield new JsonValueNull();
      }
      case BOOLEAN -> new JsonValueBoolean(reader.nextBoolean());
      case NUMBER -> new JsonValueNumber(reader.nextDouble());
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
      default ->
          throw new Json.DeserializationException("Unexpected token: " + reader.peek(), path);
    };
  }

  public static String serialize(JsonValue value) {
    try {
      var writer = new StringWriter();
      var jsonWriter = new JsonWriter(writer);
      jsonWriter.setSerializeNulls(true);
      jsonWriter.setIndent("");
      encode(jsonWriter, value, Json.Style.COMPACT);
      jsonWriter.close();
      return writer.toString();
    } catch (IOException e) {
      throw new AssertionError("In-memory IO failed", e);
    }
  }

  public static JsonValue parse(String json) throws IOException {
    var reader = new JsonReader(new StringReader(json));
    return decode(reader, Json.FieldPath.root());
  }
}
