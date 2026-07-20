package compilerxyz.codegen.util;

/** Shared name-derivation helpers used across codegen. */
public final class NameDerivation {
  private NameDerivation() {}

  public static String snakeCase(String s) {
    var sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      var c = s.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toLowerCase(c));
    }
    return sb.toString();
  }
}
