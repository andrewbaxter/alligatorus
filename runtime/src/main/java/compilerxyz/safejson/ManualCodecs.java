package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import compilerxyz.artifact.ArtifactCodecs;
import compilerxyz.util.Opt;
import java.io.IOException;

public final class ManualCodecs {
  private ManualCodecs() {}

  private static final Codec<JsonValue> JSON_VALUE = adapterToCodec(JsonValue.safeJsonAdapter);

  @SuppressWarnings("unchecked")
  public static <T> Opt<Codec<T>> lookup(Class<T> clazz) {
    if (clazz == JsonValue.class) return Opt.some((Codec<T>) (Codec<?>) JSON_VALUE);
    return ArtifactCodecs.lookup(clazz);
  }

  private static <T> Codec<T> adapterToCodec(Json.SafeJsonAdapter<T> adapter) {
    return new Codec<T>() {
      @Override
      public void codecEncode(JsonWriter writer, T value, Json.Style style) throws IOException {
        adapter.safeJsonAdapterSerialize(writer, value, style);
      }

      @Override
      public T codecDecode(JsonReader reader, Json.FieldPath path) throws IOException {
        return adapter.safeJsonAdapterDeserialize(reader, path);
      }
    };
  }
}
