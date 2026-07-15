package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import compilerxyz.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Scans handwritten runtime source for {@code @BuiltinTypeId(id)} annotations on Type classes and
 * returns a list of (snake_case name, id) specs for TypeIdsGen to emit.
 */
public final class BuiltinTypeIdScanner {
  private BuiltinTypeIdScanner() {}

  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  public record TypeIdSpec(String name, long id) {}

  public static TList<TypeIdSpec> scanRoot(Path runtimeSourceRoot) throws IOException {
    var out = new TList<TypeIdSpec>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("BuiltinTypeId")) continue;
        var cu = StaticJavaParser.parse(file);
        for (var t : cu.getTypes()) {
          collect(t, out);
        }
      }
    }
    return out;
  }

  private static void collect(TypeDeclaration<?> t, TList<TypeIdSpec> out) {
    AnnotationExpr ann = null;
    for (var a : t.getAnnotations()) {
      var n = a.getNameAsString();
      if (n.equals("BuiltinTypeId") || n.equals("compilerxyz.annotations.BuiltinTypeId")) {
        ann = a;
        break;
      }
    }
    if (ann != null) {
      out.add(new TypeIdSpec(snakeCase(t.getNameAsString()), parseIdValue(ann)));
    }
    for (var member : t.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        collect(nested, out);
      }
    }
  }

  private static long parseIdValue(AnnotationExpr ann) {
    String raw;
    if (ann instanceof SingleMemberAnnotationExpr s) {
      raw = s.getMemberValue().toString();
    } else if (ann instanceof NormalAnnotationExpr n) {
      raw = null;
      for (var p : n.getPairs()) {
        if (p.getNameAsString().equals("value")) {
          raw = p.getValue().toString();
          break;
        }
      }
      if (raw == null) throw new IllegalStateException("@BuiltinTypeId missing value");
    } else {
      throw new IllegalStateException("Unhandled @BuiltinTypeId form: " + ann);
    }
    if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
    return Long.parseLong(raw);
  }

  private static String snakeCase(String name) {
    var sb = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      var c = name.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toLowerCase(c));
    }
    return sb.toString();
  }
}
