package compilerxyz.codegen.experimental;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.Type;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.AspectsGen;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class JavaAspectExtractor {
  private JavaAspectExtractor() {}

  /**
   * Scan the given runtime source root for all files with {@code @BuiltinAspect} and extract
   * their aspect specs.
   */
  public static TList<AspectsGen.AspectSpec> scanRoot(Path runtimeSourceRoot) throws IOException {
    var out = new TList<AspectsGen.AspectSpec>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("@BuiltinAspect")) continue;
        var spec = extractOne(file);
        if (spec != null) out.add(spec);
      }
    }
    return out;
  }

  private static AspectsGen.@org.jspecify.annotations.Nullable AspectSpec extractOne(Path sourceFile)
      throws IOException {
    var cu = StaticJavaParser.parse(sourceFile);
    var ifaceDecl = findAnnotatedInterface(cu);
    if (ifaceDecl == null) return null;
    var annot = requireAnnotation(ifaceDecl, "BuiltinAspect");
    var attrs = readAttrs(annot);
    var importMap = buildImportMap(cu);

    var kind = attrs.get("kind");
    var aspectPrefix = "aspect" + kind;

    var methods = new TList<AspectsGen.AspectMethod>();
    for (var m : ifaceDecl.getMethods()) {
      if (!m.isAbstract()) continue;
      var javaName = m.getNameAsString();
      if (!javaName.startsWith(aspectPrefix)) {
        throw new IllegalStateException(
            "Method " + javaName + " does not start with expected aspect prefix " + aspectPrefix);
      }
      var afterPrefix = javaName.substring(aspectPrefix.length());
      var shortName = Character.toLowerCase(afterPrefix.charAt(0)) + afterPrefix.substring(1);
      var params = new TList<AspectsGen.Param>();
      for (var p : m.getParameters()) {
        params.add(
            new AspectsGen.Param(p.getNameAsString(), typeNameOf(p.getType(), importMap)));
      }
      methods.add(
          new AspectsGen.AspectMethod(
              "", shortName, params, typeNameOf(m.getType(), importMap)));
    }

    return new AspectsGen.AspectSpec(
        "", kind, Long.parseLong(attrs.get("uniqueId")), methods);
  }

  private static @org.jspecify.annotations.Nullable ClassOrInterfaceDeclaration
      findAnnotatedInterface(CompilationUnit cu) {
    for (var t : cu.getTypes()) {
      if (t instanceof ClassOrInterfaceDeclaration c
          && c.isInterface()
          && c.getAnnotationByName("BuiltinAspect").isPresent()) {
        return c;
      }
    }
    return null;
  }

  private static AnnotationExpr requireAnnotation(
      com.github.javaparser.ast.nodeTypes.NodeWithAnnotations<?> node, String name) {
    return node.getAnnotationByName(name)
        .orElseThrow(() -> new IllegalStateException("Missing @" + name));
  }

  private static Map<String, String> readAttrs(AnnotationExpr annot) {
    var out = new HashMap<String, String>();
    if (annot instanceof NormalAnnotationExpr n) {
      for (var p : n.getPairs()) {
        out.put(p.getNameAsString(), literalValueOf(p));
      }
    }
    return out;
  }

  private static String literalValueOf(MemberValuePair p) {
    var v = p.getValue();
    if (v instanceof StringLiteralExpr s) return s.asString();
    return v.toString().replace("L", "").replace("\"", "");
  }

  private static TMap<String, String> buildImportMap(CompilationUnit cu) {
    var m = new TMap<String, String>();
    for (var imp : cu.getImports()) {
      var full = imp.getNameAsString();
      var simple = full.substring(full.lastIndexOf('.') + 1);
      m.put(simple, full);
    }
    return m;
  }

  private static TypeName typeNameOf(Type t, TMap<String, String> imports) {
    var simple = t.asString();
    var fqn = imports.getOrNull(simple);
    if (fqn == null) fqn = "java.lang." + simple;
    var idx = fqn.lastIndexOf('.');
    return ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
  }
}
