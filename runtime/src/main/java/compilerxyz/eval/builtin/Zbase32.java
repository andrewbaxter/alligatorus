package compilerxyz.eval.builtin;

import compilerxyz.annotations.CodegenDescriptor;
import compilerxyz.gen.ListStoreByte;
import java.io.ByteArrayOutputStream;

public final class Zbase32 {
  private Zbase32() {}

  private static final String ALPHABET = "ybndrfg8ejkmcpqxot1uwisza345h769";
  private static final int[] DECODE = new int[128];

  static {
    for (var i = 0; i < DECODE.length; i++) DECODE[i] = -1;
    for (var i = 0; i < ALPHABET.length(); i++) DECODE[ALPHABET.charAt(i)] = i;
  }

  public static byte[] decode(String encoded) {
    var out = new ByteArrayOutputStream(encoded.length() * 5 / 8 + 1);
    var buffer = 0;
    var bits = 0;
    for (var i = 0; i < encoded.length(); i++) {
      var c = encoded.charAt(i);
      if (c >= DECODE.length || DECODE[c] < 0) {
        throw new IllegalArgumentException("zbase32.decode: invalid character '" + c + "'");
      }
      buffer = (buffer << 5) | DECODE[c];
      bits += 5;
      if (bits >= 8) {
        bits -= 8;
        out.write((buffer >> bits) & 0xFF);
      }
    }
    return out.toByteArray();
  }

  public static String encode(byte[] data) {
    var sb = new StringBuilder();
    var buffer = 0;
    var bits = 0;
    for (var b : data) {
      buffer = (buffer << 8) | (b & 0xFF);
      bits += 8;
      while (bits >= 5) {
        bits -= 5;
        sb.append(ALPHABET.charAt((buffer >> bits) & 31));
      }
    }
    if (bits > 0) {
      sb.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
    }
    return sb.toString();
  }

  @CodegenDescriptor
  public static ListStoreByte decodeToStore(String encoded) {
    var bytes = decode(encoded);
    var list = new ListStoreByte();
    for (var b : bytes) list.push(b);
    return list;
  }
}
