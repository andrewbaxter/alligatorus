package compilerxyz.main.safejson;

import com.google.gson.stream.JsonWriter;
import compilerxyz.prelude.util.Opt;
import java.io.IOException;

public final class Json {

    public static final int CANONICAL_EXP_LEN_LIMIT = 21;

    private Json() {}

    public enum Style {
        COMPACT,
        PRETTY,
        CANONICAL
    }

    public static void serializeDouble(JsonWriter writer, double value, Style style) throws IOException {
        if (style == Style.CANONICAL) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new SafeJsonException("Cannot serialize NaN or Infinity in canonical JSON");
            }
            if (value == Math.floor(value) && Math.abs(value) < (1L << 53)) {
                writer.value((long) value);
            } else {
                double d = value;
                boolean negative = d < 0;
                if (negative) d = -d;
                String string = Double.toString(d);
                int idx = string.indexOf('E');
                String part, fracPart;
                int exponentValue;
                if (idx >= 0) {
                    exponentValue = Integer.parseInt(string.substring(idx + 1));
                    String mantissa = string.substring(0, idx);
                    int dotIdx = mantissa.indexOf('.');
                    if (dotIdx >= 0) {
                        part = mantissa.substring(0, dotIdx);
                        fracPart = mantissa.substring(dotIdx + 1);
                    } else {
                        part = mantissa;
                        fracPart = "";
                    }
                } else {
                    exponentValue = 0;
                    int dotIdx = string.indexOf('.');
                    if (dotIdx >= 0) {
                        part = string.substring(0, dotIdx);
                        fracPart = string.substring(dotIdx + 1);
                    } else {
                        part = string;
                        fracPart = "";
                    }
                }
                String digits = part + fracPart;
                int k = digits.length();
                while (k > 1 && digits.charAt(k - 1) == '0') k--;
                String trimmedDigits = digits.substring(0, k);
                int beforeDecimal = part.length() + exponentValue;
                var sb = new StringBuilder();
                if (negative) sb.append('-');
                if (k <= beforeDecimal && beforeDecimal <= CANONICAL_EXP_LEN_LIMIT) {
                    sb.append(trimmedDigits);
                    sb.append("0".repeat(beforeDecimal - k));
                } else if (0 < beforeDecimal && beforeDecimal <= CANONICAL_EXP_LEN_LIMIT) {
                    sb.append(trimmedDigits, 0, beforeDecimal);
                    sb.append('.');
                    sb.append(trimmedDigits, beforeDecimal, k);
                } else if (-6 < beforeDecimal && beforeDecimal <= 0) {
                    sb.append("0.");
                    sb.append("0".repeat(-beforeDecimal));
                    sb.append(trimmedDigits);
                } else {
                    sb.append(trimmedDigits.charAt(0));
                    if (k > 1) {
                        sb.append('.');
                        sb.append(trimmedDigits, 1, k);
                    }
                    int exp = beforeDecimal - 1;
                    sb.append('e');
                    if (exp > 0) sb.append('+');
                    sb.append(exp);
                }
                writer.jsonValue(sb.toString());
            }
        } else {
            writer.value(value);
        }
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
        private final Opt<FieldPath> parent;

        private FieldPath(String segment, Opt<FieldPath> parent) {
            this.segment = segment;
            this.parent = parent;
        }

        public static FieldPath root() {
            return new FieldPath("$", Opt.none());
        }

        public FieldPath field(String fieldName) {
            return new FieldPath(fieldName, Opt.some(this));
        }

        public FieldPath index(int index) {
            return new FieldPath("[" + index + "]", Opt.some(this));
        }

        @Override
        public String toString() {
            if (parent.optIsNone()) {
                return segment;
            }
            var parentStr = parent.optUnwrap().toString();
            if (segment.startsWith("[")) {
                return parentStr + segment;
            }
            return parentStr + "." + segment;
        }
    }

    public static class DeserializationException extends RuntimeException {
        public DeserializationException(String message, FieldPath path) {
            super(message + " at " + path);
        }

        public DeserializationException(String message, FieldPath path, Throwable cause) {
            super(message + " at " + path, cause);
        }
    }
}
