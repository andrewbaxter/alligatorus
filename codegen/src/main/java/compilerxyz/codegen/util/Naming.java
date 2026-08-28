package compilerxyz.codegen.util;

public final class Naming {
    private Naming() {}

    public static String snakeCase(String s) {
        var out = new StringBuilder();
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                out.append('_');
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }
}
