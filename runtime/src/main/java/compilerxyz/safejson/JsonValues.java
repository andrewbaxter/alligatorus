package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.jspecify.annotations.Nullable;

public final class JsonValues {
  private JsonValues() {}

  public static JsonValue toValue(@Nullable Object object) {
    try {
      var serialized = Json.serialize(object, Json.Style.COMPACT);
      var reader = new JsonReader(new StringReader(serialized));
      return JsonValue.safeJsonAdapter.deserialize(reader, Json.FieldPath.root());
    } catch (IOException e) {
      throw new AssertionError("In-memory IO failed", e);
    }
  }

  public static <T> T fromValue(Class<T> clazz, JsonValue value) {
    try {
      var stringWriter = new StringWriter();
      var writer = new JsonWriter(stringWriter);
      writer.setSerializeNulls(true);
      JsonValue.safeJsonAdapter.serialize(writer, value, Json.Style.COMPACT);
      writer.close();
      return Json.deserialize(clazz, stringWriter.toString());
    } catch (IOException e) {
      throw new AssertionError("In-memory IO failed", e);
    }
  }

  public static <T> @Nullable T fromValueNullable(Class<T> clazz, JsonValue value) {
    return switch (value) {
      case JsonValueNull n -> null;
      case JsonValueString s -> fromValue(clazz, value);
      case JsonValueNumber n -> fromValue(clazz, value);
      case JsonValueBoolean b -> fromValue(clazz, value);
      case JsonValueArray a -> fromValue(clazz, value);
      case JsonValueObject o -> fromValue(clazz, value);
    };
  }
}
