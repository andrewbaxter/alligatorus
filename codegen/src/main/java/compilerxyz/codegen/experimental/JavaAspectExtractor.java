package compilerxyz.codegen.experimental;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
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
    var javadoc = attrs.getOrDefault("javadoc", "");

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
        var nullable = p.getAnnotationByName("Nullable").isPresent();
        params.add(
            new AspectsGen.Param(
                p.getNameAsString(), typeNameOf(p.getType(), importMap), nullable));
      }
      methods.add(
          new AspectsGen.AspectMethod(
              "", shortName, params, typeNameOf(m.getType(), importMap)));
    }

    return new AspectsGen.AspectSpec(
        javadoc, kind, Long.parseLong(attrs.get("uniqueId")), methods);
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
    var raw = v.toString();
    if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
    return raw;
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
    if (t instanceof PrimitiveType p) {
      return switch (p.getType()) {
        case BOOLEAN -> TypeName.BOOLEAN;
        case DOUBLE -> TypeName.DOUBLE;
        case INT -> TypeName.INT;
        case LONG -> TypeName.LONG;
        case FLOAT -> TypeName.FLOAT;
        case CHAR -> TypeName.CHAR;
        case BYTE -> TypeName.BYTE;
        case SHORT -> TypeName.SHORT;
      };
    }
    if (t instanceof ClassOrInterfaceType c) {
      var raw = classNameOf(c.getNameAsString(), imports);
      if (c.getTypeArguments().isPresent() && !c.getTypeArguments().get().isEmpty()) {
        var argsList = c.getTypeArguments().get();
        var argNames = new TypeName[argsList.size()];
        for (int i = 0; i < argsList.size(); i++) {
          argNames[i] = typeNameOf(argsList.get(i), imports);
        }
        return ParameterizedTypeName.get(raw, argNames);
      }
      return raw;
    }
    throw new IllegalStateException("Unhandled type: " + t + " (" + t.getClass() + ")");
  }

  private static ClassName classNameOf(String simple, TMap<String, String> imports) {
    var fqn = imports.getOrNull(simple);
    if (fqn == null) fqn = "java.lang." + simple;
    var idx = fqn.lastIndexOf('.');
    return ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
  }
}
