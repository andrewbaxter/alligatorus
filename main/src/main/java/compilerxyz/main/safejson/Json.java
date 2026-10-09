package compilerxyz.main.safejson;

import compilerxyz.prelude.util.Opt;

public final class Json {

    private Json() {}

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
