package compilerxyz.codegen.util;

public record Method(String baseName, TypeRef retType, MethodArgs args) {

  public String sourceName() {
    return toSnakeCase(baseName);
  }

  public String hostName(String interfacePrefix) {
    return interfacePrefix + capitalize(baseName);
  }

  private static String toSnakeCase(String camelCase) {
    var out = new StringBuilder();
    for (var i = 0; i < camelCase.length(); i++) {
      var c = camelCase.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) out.append('_');
        out.append(Character.toLowerCase(c));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  private static String capitalize(String s) {
    if (s.isEmpty()) return s;
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
