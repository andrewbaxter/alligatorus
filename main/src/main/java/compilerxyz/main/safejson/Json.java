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
                double _d = value;
                boolean _negative = _d < 0;
                if (_negative) _d = -_d;
                String _originalString = Double.toString(_d);
                int _eIdx = _originalString.indexOf('E');
                String _intPart, _fracPart;
                int _exponentValue;
                if (_eIdx >= 0) {
                    _exponentValue = Integer.parseInt(_originalString.substring(_eIdx + 1));
                    String _mantissa = _originalString.substring(0, _eIdx);
                    int _dotIdx = _mantissa.indexOf('.');
                    if (_dotIdx >= 0) {
                        _intPart = _mantissa.substring(0, _dotIdx);
                        _fracPart = _mantissa.substring(_dotIdx + 1);
                    } else {
                        _intPart = _mantissa;
                        _fracPart = "";
                    }
                } else {
                    _exponentValue = 0;
                    int _dotIdx = _originalString.indexOf('.');
                    if (_dotIdx >= 0) {
                        _intPart = _originalString.substring(0, _dotIdx);
                        _fracPart = _originalString.substring(_dotIdx + 1);
                    } else {
                        _intPart = _originalString;
                        _fracPart = "";
                    }
                }
                String _allDigits = _intPart + _fracPart;
                int _k = _allDigits.length();
                while (_k > 1 && _allDigits.charAt(_k - 1) == '0') _k--;
                String _digits = _allDigits.substring(0, _k);
                int _digitsBeforeDecimal = _intPart.length() + _exponentValue;
                var _sb = new StringBuilder();
                if (_negative) _sb.append('-');
                if (_k <= _digitsBeforeDecimal && _digitsBeforeDecimal <= CANONICAL_EXP_LEN_LIMIT) {
                    _sb.append(_digits);
                    _sb.append("0".repeat(_digitsBeforeDecimal - _k));
                } else if (0 < _digitsBeforeDecimal && _digitsBeforeDecimal <= CANONICAL_EXP_LEN_LIMIT) {
                    _sb.append(_digits, 0, _digitsBeforeDecimal);
                    _sb.append('.');
                    _sb.append(_digits, _digitsBeforeDecimal, _k);
                } else if (-6 < _digitsBeforeDecimal && _digitsBeforeDecimal <= 0) {
                    _sb.append("0.");
                    _sb.append("0".repeat(-_digitsBeforeDecimal));
                    _sb.append(_digits);
                } else {
                    _sb.append(_digits.charAt(0));
                    if (_k > 1) {
                        _sb.append('.');
                        _sb.append(_digits, 1, _k);
                    }
                    int _exp = _digitsBeforeDecimal - 1;
                    _sb.append('e');
                    if (_exp > 0) _sb.append('+');
                    _sb.append(_exp);
                }
                writer.jsonValue(_sb.toString());
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
    }
}
