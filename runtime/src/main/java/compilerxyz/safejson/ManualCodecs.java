package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import compilerxyz.artifact.ArtifactCodecs;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

public final class ManualCodecs {
  private ManualCodecs() {}

  private static final Codec<JsonValue> JSON_VALUE = adapterToCodec(JsonValue.safeJsonAdapter);

  @SuppressWarnings("unchecked")
  public static <T> @Nullable Codec<T> lookup(Class<T> clazz) {
    if (clazz == JsonValue.class) return (Codec<T>) (Codec<?>) JSON_VALUE;
    return ArtifactCodecs.lookup(clazz);
  }

  private static <T> Codec<T> adapterToCodec(Json.SafeJsonAdapter<T> adapter) {
    return new Codec<T>() {
      @Override
      public void codecEncode(JsonWriter writer, T value, Json.Style style) throws IOException {
        adapter.serialize(writer, value, style);
      }

      @Override
      public T codecDecode(JsonReader reader, Json.FieldPath path) throws IOException {
        return adapter.deserialize(reader, path);
      }
    };
  }
}
