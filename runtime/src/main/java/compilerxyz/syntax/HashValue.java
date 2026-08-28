package compilerxyz.syntax;

import java.util.HexFormat;

public sealed interface HashValue {
  record Sha256(byte[] digest) implements HashValue {
    @Override
    public String hashValueHashKey() {
      return "sha256:" + HexFormat.of().formatHex(digest);
    }
  }

  String hashValueHashKey();
}
