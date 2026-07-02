package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public interface Codec<T> {
  void codecEncode(JsonWriter writer, T value, Json.Style style) throws IOException;

  T codecDecode(JsonReader reader, Json.FieldPath path) throws IOException;
}
