package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import compilerxyz.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class BuiltinIdentityScanner {
  private BuiltinIdentityScanner() {}

  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  public record IdentitySpec(String name, long value) {}

  public static TList<IdentitySpec> scanRoot(Path runtimeSourceRoot) throws IOException {
    var out = new TList<IdentitySpec>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("BuiltinIdentity")) continue;
        var cu = StaticJavaParser.parse(file);
        for (var t : cu.getTypes()) {
          collect(t, out);
        }
      }
    }
    return out;
  }

  private static void collect(TypeDeclaration<?> t, TList<IdentitySpec> out) {
    for (var a : t.getAnnotations()) {
      var n = a.getNameAsString();
      if (n.equals("BuiltinIdentity") || n.equals("compilerxyz.annotations.BuiltinIdentity")) {
        String name = null;
        Long value = null;
        if (a instanceof NormalAnnotationExpr n2) {
          for (var p : n2.getPairs()) {
            switch (p.getNameAsString()) {
              case "name" -> name = ((StringLiteralExpr) p.getValue()).asString();
              case "value" -> value = parseLong(p);
            }
          }
        }
        if (name == null || value == null) {
          throw new IllegalStateException("@BuiltinIdentity missing name or value on " + t.getNameAsString());
        }
        out.add(new IdentitySpec(name, value));
      }
    }
    for (var member : t.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) collect(nested, out);
    }
  }

  private static long parseLong(MemberValuePair p) {
    var raw = p.getValue().toString();
    if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
    return Long.parseLong(raw);
  }
}
