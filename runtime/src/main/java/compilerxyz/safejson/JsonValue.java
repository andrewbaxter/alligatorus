package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeMap;

public sealed interface JsonValue
    permits JsonValueNull,
        JsonValueBoolean,
        JsonValueNumber,
        JsonValueString,
        JsonValueArray,
        JsonValueObject {
  Json.SafeJsonAdapter<JsonValue> safeJsonAdapter = new ValueAdapter();
}

class ValueAdapter implements Json.SafeJsonAdapter<JsonValue> {
  @Override
  public void safeJsonAdapterSerialize(JsonWriter writer, JsonValue value, Json.Style style) throws IOException {
    switch (value) {
      case JsonValueNull ignored -> writer.nullValue();
      case JsonValueBoolean b -> writer.value(b.value());
      case JsonValueNumber n -> Json.serializeDouble(writer, n.value(), style);
      case JsonValueString s -> writer.value(s.value());
      case JsonValueArray a -> {
        writer.beginArray();
        for (var element : a.elements()) {
          safeJsonAdapterSerialize(writer, element, style);
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
            safeJsonAdapterSerialize(writer, entry.getValue(), style);
          }
        } else {
          for (var entry : entries.entrySet()) {
            writer.name(entry.getKey());
            safeJsonAdapterSerialize(writer, entry.getValue(), style);
          }
        }
        writer.endObject();
      }
    }
  }

  @Override
  public JsonValue safeJsonAdapterDeserialize(JsonReader reader, Json.FieldPath path) throws IOException {
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
          elements.add(safeJsonAdapterDeserialize(reader, path.index(index++)));
        }
        reader.endArray();
        yield new JsonValueArray(elements);
      }
      case BEGIN_OBJECT -> {
        reader.beginObject();
        var entries = new TOrderedMap<String, JsonValue>();
        while (reader.hasNext()) {
          var key = reader.nextName();
          entries.put(key, safeJsonAdapterDeserialize(reader, path.key(key)));
        }
        reader.endObject();
        yield new JsonValueObject(entries);
      }
      default ->
          throw new Json.DeserializationException("Unexpected token: " + reader.peek(), path);
    };
  }
}
