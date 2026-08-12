package compilerxyz.syntax;

import java.util.HexFormat;

public sealed interface HashValue {
  record Sha256(byte[] digest) implements HashValue {}

  default String hashValueHashKey() {
    return switch (this) {
      case Sha256 s -> "sha256:" + HexFormat.of().formatHex(s.digest());
    };
  }
}
