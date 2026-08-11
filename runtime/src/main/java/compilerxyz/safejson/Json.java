package compilerxyz.safejson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import compilerxyz.gen.codec.Codecs;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.jspecify.annotations.Nullable;

public final class Json {

  public static final int CANONICAL_EXP_LEN_LIMIT = 21;

  private Json() {}

  public enum Style {
    COMPACT,
    PRETTY,
    CANONICAL
  }

  public interface SafeJsonAdapter<T> {
    void serialize(JsonWriter writer, T value, Style style) throws IOException;

    T deserialize(JsonReader reader, FieldPath path) throws IOException;
  }

  @SuppressWarnings("unchecked")
  public static void encodeValue(JsonWriter writer, @Nullable Object object, Style style)
      throws IOException {
    if (object == null) {
      writer.nullValue();
      return;
    }
    var codec = (Codec<Object>) (Codec<?>) Codecs.lookup(object.getClass());
    if (codec == null) {
      throw new SafeJsonException(
          "No registered Codec for runtime class " + object.getClass().getName()
              + " — mark the type with SafeJsonSerializable or register a hand-coded codec.");
    }
    codec.codecEncode(writer, object, style);
  }

  @SuppressWarnings("unchecked")
  public static void encodeValue(
      JsonWriter writer, @Nullable Object object, Class<?> declaredClass, Style style)
      throws IOException {
    if (object == null) {
      writer.nullValue();
      return;
    }
    var codec = (Codec<Object>) (Codec<?>) Codecs.lookup(declaredClass);
    if (codec != null) {
      codec.codecEncode(writer, object, style);
    } else {
      encodeValue(writer, object, style);
    }
  }

  public static <T> T decodeValue(JsonReader reader, Class<T> clazz, FieldPath path)
      throws IOException {
    var codec = Codecs.lookup(clazz);
    if (codec == null) {
      throw new SafeJsonException(
          "No registered Codec for class " + clazz.getName()
              + " — mark the type with SafeJsonSerializable or register a hand-coded codec.");
    }
    return codec.codecDecode(reader, path);
  }

  public static <T> String serialize(@Nullable T object, Style style) throws IOException {
    var stringWriter = new StringWriter();
    var writer = new JsonWriter(stringWriter);
    writer.setSerializeNulls(true);
    switch (style) {
      case COMPACT, CANONICAL -> writer.setIndent("");
      case PRETTY -> writer.setIndent("  ");
    }
    if (object == null) {
      writer.nullValue();
    } else {
      encodeValue(writer, object, style);
    }
    writer.close();
    return stringWriter.toString();
  }

  public static <T> T deserialize(Class<T> clazz, String json) throws IOException {
    var reader = new JsonReader(new StringReader(json));
    var path = FieldPath.root();
    return decodeValue(reader, clazz, path);
  }

  public static void serializeDouble(JsonWriter writer, double value, Style style)
      throws IOException {
    if (style == Style.CANONICAL) {
      if (Double.isNaN(value) || Double.isInfinite(value)) {
        throw new SafeJsonException("Cannot serialize NaN or Infinity in canonical JSON");
      }
      if (value == Math.floor(value) && Math.abs(value) < (1L << 53)) {
        writer.value((long) value);
      } else {
        writer.jsonValue(doubleToEs6(value));
      }
    } else {
      writer.value(value);
    }
  }

  static String doubleToEs6(double value) {
    boolean negative = value < 0;
    if (negative) value = -value;

    String originalString = Double.toString(value);

    int eIdx = originalString.indexOf('E');
    String intPart, fracPart;
    int exponentValue;

    if (eIdx >= 0) {
      exponentValue = Integer.parseInt(originalString.substring(eIdx + 1));
      String mantissa = originalString.substring(0, eIdx);
      int dotIdx = mantissa.indexOf('.');
      if (dotIdx >= 0) {
        intPart = mantissa.substring(0, dotIdx);
        fracPart = mantissa.substring(dotIdx + 1);
      } else {
        intPart = mantissa;
        fracPart = "";
      }
    } else {
      exponentValue = 0;
      int dotIdx = originalString.indexOf('.');
      if (dotIdx >= 0) {
        intPart = originalString.substring(0, dotIdx);
        fracPart = originalString.substring(dotIdx + 1);
      } else {
        intPart = originalString;
        fracPart = "";
      }
    }

    String allDigits = intPart + fracPart;

    int k = allDigits.length();
    while (k > 1 && allDigits.charAt(k - 1) == '0') k--;
    String digits = allDigits.substring(0, k);

    int digitsBeforeDecimal = intPart.length() + exponentValue;

    var result = new StringBuilder();
    if (negative) result.append('-');

    if (k <= digitsBeforeDecimal && digitsBeforeDecimal <= CANONICAL_EXP_LEN_LIMIT) {

      result.append(digits);
      result.append("0".repeat(digitsBeforeDecimal - k));
    } else if (0 < digitsBeforeDecimal && digitsBeforeDecimal <= CANONICAL_EXP_LEN_LIMIT) {

      result.append(digits, 0, digitsBeforeDecimal);
      result.append('.');
      result.append(digits, digitsBeforeDecimal, k);
    } else if (-6 < digitsBeforeDecimal && digitsBeforeDecimal <= 0) {

      result.append("0.");
      result.append("0".repeat(-digitsBeforeDecimal));
      result.append(digits);
    } else {

      result.append(digits.charAt(0));
      if (k > 1) {
        result.append('.');
        result.append(digits, 1, k);
      }
      int exp = digitsBeforeDecimal - 1;
      result.append('e');
      if (exp > 0) result.append('+');
      result.append(exp);
    }

    return result.toString();
  }

  public static class SafeJsonException extends RuntimeException {
    public SafeJsonException(String message) {
      super(message);
    }

    public SafeJsonException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static final class FieldPath {
    private final String segment;
    private final @Nullable FieldPath parent;

    private FieldPath(String segment, @Nullable FieldPath parent) {
      this.segment = segment;
      this.parent = parent;
    }

    public static FieldPath root() {
      return new FieldPath("$", null);
    }

    public FieldPath field(String fieldName) {
      return new FieldPath(fieldName, this);
    }

    public FieldPath index(int index) {
      return new FieldPath("[" + index + "]", this);
    }

    public FieldPath key(String key) {
      return new FieldPath("[\"" + key + "\"]", this);
    }

    @Override
    public String toString() {
      if (parent == null) {
        return segment;
      }
      var parentStr = parent.toString();
      if (segment.startsWith("[")) {
        return parentStr + segment;
      }
      return parentStr + "." + segment;
    }
  }

  public static class DeserializationException extends RuntimeException {
    private final String errorMessage;
    private final FieldPath path;

    public DeserializationException(String message, FieldPath path) {
      super(message + " at " + path);
      this.errorMessage = message;
      this.path = path;
    }

    public DeserializationException(String message, FieldPath path, Throwable cause) {
      super(message + " at " + path, cause);
      this.errorMessage = message;
      this.path = path;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public FieldPath getPath() {
      return path;
    }
  }
}
