package compilerxyz.main.eval;

import compilerxyz.main.eval.value.constkey.TypeConstkeyDynamic;
import java.util.Comparator;

public sealed interface ConstKey {
    record Str(String value) implements ConstKey {
        @Override
        public String constKeyToCanonical() {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    record Uint(long value) implements ConstKey {
        @Override
        public String constKeyToCanonical() {
            return Long.toUnsignedString(value);
        }
    }

    Comparator<ConstKey> CANONICAL_ORDER = new Comparator<>() {
        @Override
        public int compare(ConstKey a, ConstKey b) {
            return a.constKeyToCanonical().compareTo(b.constKeyToCanonical());
        }
    };

    String constKeyToCanonical();

    static Type dynamicType() {
        return new TypeConstkeyDynamic();
    }
}
