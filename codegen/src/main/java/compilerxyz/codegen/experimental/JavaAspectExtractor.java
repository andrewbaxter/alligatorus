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
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.Method;
import compilerxyz.codegen.util.MethodArgs;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class JavaAspectExtractor {
  private JavaAspectExtractor() {}

  public static InterfaceSpec extract(Path sourceFile) throws Exception {
    var cu = StaticJavaParser.parse(sourceFile);
    var ifaceDecl = findAnnotatedInterface(cu);
    var annot = requireAnnotation(ifaceDecl, "BuiltinAspect");
    var attrs = readAttrs(annot);

    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var importMap = buildImportMap(cu);

    var methods = new TList<Method>();
    for (var m : ifaceDecl.getMethods()) {
      var mAnn = requireAnnotation(m, "BuiltinAspectMethod");
      var methodAttrs = readAttrs(mAnn);
      methods.add(
          new Method(
              methodAttrs.get("name"),
              typeRefOf(m.getType(), importMap, pkg),
              new MethodArgs(argsOf(m, importMap, pkg))));
    }

    return new InterfaceSpec(
        attrs.get("name"),
        parseInt(attrs.get("uniqueId")),
        parseInt(attrs.get("scopeId")),
        attrs.get("fieldPrefix"),
        attrs.get("kind"),
        null,
        true,
        null,
        null,
        pkg + "." + ifaceDecl.getNameAsString(),
        methods);
  }

  private static ClassOrInterfaceDeclaration findAnnotatedInterface(CompilationUnit cu) {
    for (var t : cu.getTypes()) {
      if (t instanceof ClassOrInterfaceDeclaration c && c.isInterface()
          && c.getAnnotationByName("BuiltinAspect").isPresent()) {
        return c;
      }
    }
    throw new IllegalStateException("No @BuiltinAspect interface found");
  }

  private static AnnotationExpr requireAnnotation(
      com.github.javaparser.ast.nodeTypes.NodeWithAnnotations<?> node, String name) {
    return node.getAnnotationByName(name).orElseThrow(
        () -> new IllegalStateException("Missing @" + name));
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

  private static int parseInt(String s) {
    return Integer.parseInt(s);
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

  private static TypeRef typeRefOf(Type t, TMap<String, String> imports, String pkg) {
    var simple = t.asString();
    var fqn = imports.getOrNull(simple);
    if (fqn == null) fqn = "java.lang." + simple;
    var idx = fqn.lastIndexOf('.');
    return TypeRef.leaf(ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1)));
  }

  private static TList<NamedTypeRef> argsOf(
      MethodDeclaration m, TMap<String, String> imports, String pkg) {
    var out = new TList<NamedTypeRef>();
    for (var p : m.getParameters()) {
      out.add(new NamedTypeRef(p.getNameAsString(), typeRefOf(p.getType(), imports, pkg)));
    }
    return out;
  }
}
