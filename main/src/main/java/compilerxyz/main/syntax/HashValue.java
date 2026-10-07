package compilerxyz.main.syntax;

import java.util.HexFormat;

public sealed interface HashValue {
    record Sha256(byte[] digest) implements HashValue {
        @Override
        public String hashValueHashKey() {
            return "sha256:" + HexFormat.of().formatHex(digest);
        }
    }

    String hashValueHashKey();

    static HashValue hashValueFromKey(String key) {
        if (!key.startsWith("sha256:")) throw new IllegalArgumentException("unknown hash key: " + key);
        return new Sha256(HexFormat.of().parseHex(key.substring("sha256:".length())));
    }
}
