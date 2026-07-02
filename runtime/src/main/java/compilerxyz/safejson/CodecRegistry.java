package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import compilerxyz.artifact.ArtifactCodecs;
import compilerxyz.util.TMap;

public final class CodecRegistry {
  private CodecRegistry() {}

  private static final TMap<Class<?>, Codec<?>> BY_CLASS = new TMap<>();

  public static <T> void register(Class<T> clazz, Codec<T> codec) {
    BY_CLASS.put(clazz, codec);
  }

  @SuppressWarnings("unchecked")
  public static <T> @Nullable Codec<T> lookup(Class<T> clazz) {
    return (Codec<T>) BY_CLASS.get(clazz);
  }

  static {

    register(JsonValue.class, adapterToCodec(JsonValue.safeJsonAdapter));

    ArtifactCodecs.registerAll();

    var initClassName = "compilerxyz.gen.codec.CodecsInit";
    try {
      Class.forName(initClassName, true, CodecRegistry.class.getClassLoader());
    } catch (ClassNotFoundException ignored) {

    }
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
