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
      var _sw = new StringWriter();
      var _jw = new JsonWriter(_sw);
      _jw.setSerializeNulls(true);
      _jw.setIndent("");
      if (object == null) {
        _jw.nullValue();
      } else {
        Json.encodeValue(_jw, object, Json.Style.COMPACT);
      }
      _jw.close();
      var serialized = _sw.toString();
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

}
