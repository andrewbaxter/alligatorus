package compilerxyz.main.syntax;

import compilerxyz.prelude.annotations.BuiltinFull;
import java.util.HexFormat;

@BuiltinFull(uniqueId = 6529L)
public sealed interface HashValue {
    @BuiltinFull(uniqueId = 6530L)
    record Sha256(byte[] digest) implements HashValue {
        @Override
        public String hashValueHashKey() {
            return "sha256:" + HexFormat.of().formatHex(digest);
        }
    }

    String hashValueHashKey();
}
