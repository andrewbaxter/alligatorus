package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Discovers every class/interface/record under the runtime source root that exposes a nullary
 * {@code public static dynamicType()}. Returns a map from the enclosing type's ClassName to the
 * CodeBlock that constructs its runtime {@code Type} descriptor. BuiltinInterfacesGen (and
 * other generators) use this instead of a hand-maintained leaf-descriptor table.
 */
public final class DynamicTypeScanner {
  private DynamicTypeScanner() {}

  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  public static TMap<ClassName, CodeBlock> scanRoot(Path runtimeSourceRoot) throws IOException {
    var out = new TMap<ClassName, CodeBlock>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("dynamicType")) continue;
        var cu = StaticJavaParser.parse(file);
        var pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        for (var t : cu.getTypes()) {
          walk(pkg, t, out);
        }
      }
    }
    // Mirror compilerxyz.handwritten.AspectXxx.* entries to compilerxyz.gen.AspectXxx.* so
    // aspect-emitter descriptor resolution can find nested records whose runtime Type is defined
    // via a delegating dynamicType() in the handwritten source.
    var mirror = new TMap<ClassName, CodeBlock>();
    for (var e : out.entrySet()) {
      var cn = e.getKey();
      if (!cn.packageName().equals("compilerxyz.handwritten")) continue;
      if (cn.simpleNames().size() < 2) continue;
      var mirrored = ClassName.get(
          "compilerxyz.gen",
          cn.simpleNames().get(0),
          cn.simpleNames().subList(1, cn.simpleNames().size()).toArray(new String[0]));
      mirror.put(mirrored, e.getValue());
    }
    for (var e : mirror.entrySet()) out.put(e.getKey(), e.getValue());
    return out;
  }

  private static void walk(String pkg, TypeDeclaration<?> t, TMap<ClassName, CodeBlock> out) {
    var simple = t.getNameAsString();
    if (hasNullaryDynamicType(t)) {
      var cn = ClassName.get(pkg, simple);
      out.put(cn, CodeBlock.of("$T.dynamicType()", cn));
    }
    // Recurse into nested types — they're addressable as OuterName.NestedName.
    for (var member : t.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        walkNested(pkg, simple, nested, out);
      }
    }
  }

  private static void walkNested(
      String pkg, String enclosingSimpleChain, TypeDeclaration<?> t, TMap<ClassName, CodeBlock> out) {
    var simple = t.getNameAsString();
    if (hasNullaryDynamicType(t)) {
      var cn = ClassName.get(pkg, enclosingSimpleChain, simple);
      out.put(cn, CodeBlock.of("$T.dynamicType()", cn));
    }
    for (var member : t.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        walkNested(pkg, enclosingSimpleChain + "." + simple, nested, out);
      }
    }
  }

  private static boolean hasNullaryDynamicType(TypeDeclaration<?> t) {
    if (t instanceof ClassOrInterfaceDeclaration
        || t instanceof RecordDeclaration
        || t.isEnumDeclaration()) {
      for (var m : t.getMethods()) {
        if (isNullaryStaticDynamicType(m)) return true;
      }
    }
    return false;
  }

  private static boolean isNullaryStaticDynamicType(MethodDeclaration m) {
    if (!m.getNameAsString().equals("dynamicType")) return false;
    if (!m.isStatic()) return false;
    if (!m.getParameters().isEmpty()) return false;
    return true;
  }
}
