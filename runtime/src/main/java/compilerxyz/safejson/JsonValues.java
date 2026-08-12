package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import compilerxyz.util.Opt;

public final class JsonValues {
  private JsonValues() {}

  public static JsonValue toValue(Object object) {
    try {
      var serialized = Json.serialize(object, Json.Style.COMPACT);
      var reader = new JsonReader(new StringReader(serialized));
      return JsonValue.safeJsonAdapter.safeJsonAdapterDeserialize(reader, Json.FieldPath.root());
    } catch (IOException e) {
      throw new AssertionError("In-memory IO failed", e);
    }
  }

  public static <T> T fromValue(Class<T> clazz, JsonValue value) {
    try {
      var stringWriter = new StringWriter();
      var writer = new JsonWriter(stringWriter);
      writer.setSerializeNulls(true);
      JsonValue.safeJsonAdapter.safeJsonAdapterSerialize(writer, value, Json.Style.COMPACT);
      writer.close();
      return Json.deserialize(clazz, stringWriter.toString());
    } catch (IOException e) {
      throw new AssertionError("In-memory IO failed", e);
    }
  }

  public static <T> Opt<T> fromValueNullable(Class<T> clazz, JsonValue value) {
    return switch (value) {
      case JsonValueNull n -> Opt.none();
      case JsonValueString s -> Opt.some(fromValue(clazz, value));
      case JsonValueNumber n -> Opt.some(fromValue(clazz, value));
      case JsonValueBoolean b -> Opt.some(fromValue(clazz, value));
      case JsonValueArray a -> Opt.some(fromValue(clazz, value));
      case JsonValueObject o -> Opt.some(fromValue(clazz, value));
    };
  }
}
