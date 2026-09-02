package compilerxyz.codegen.util;

public final class NameDerivation {
    private NameDerivation() {}

    public static String snakeCase(String s) {
        var sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        var out = sb.toString();
        return out.endsWith("_") ? out.substring(0, out.length() - 1) : out;
    }
}
