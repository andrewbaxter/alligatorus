package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.Type;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Scans runtime source for methods annotated {@code @CodegenDescriptor} and produces one entry per
 * method (owner, generated constant name, method name, resolved return/param types, staticness),
 * from which {@link CodegenDescriptorGen} generates {@code JavaStaticMethodRef} /
 * {@code JavaVirtualMethodRef} constants.
 */
public final class CodegenDescriptorScanner {
  private CodegenDescriptorScanner() {}

  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  public record Entry(
      ClassName owner,
      String constantName,
      String methodName,
      TypeName returnType,
      TList<TypeName> paramTypes,
      boolean isStatic,
      boolean ownerIsInterface) {}

  public static TList<Entry> scanRoot(Path runtimeSourceRoot) throws IOException {
    var out = new TList<Entry>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("CodegenDescriptor")) continue;
        var cu = StaticJavaParser.parse(file);
        var pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        var imports = new TMap<String, String>();
        for (var imp : cu.getImports()) {
          if (imp.isAsterisk() || imp.isStatic()) continue;
          var full = imp.getNameAsString();
          imports.put(full.substring(full.lastIndexOf('.') + 1), full);
        }
        for (var t : cu.getTypes()) {
          walkType(pkg, "", t, imports, out);
        }
      }
    }
    return out;
  }

  private static void walkType(
      String pkg,
      String enclosing,
      TypeDeclaration<?> t,
      TMap<String, String> imports,
      TList<Entry> out) {
    var simple = t.getNameAsString();
    if (t instanceof ClassOrInterfaceDeclaration || t instanceof RecordDeclaration) {
      boolean ownerIsInterface =
          t instanceof ClassOrInterfaceDeclaration ci && ci.isInterface();
      var owner =
          enclosing.isEmpty() ? ClassName.get(pkg, simple) : ClassName.get(pkg, enclosing, simple);
      var classTypeParams = new TSet<String>();
      if (t instanceof ClassOrInterfaceDeclaration ci) {
        for (var tp : ci.getTypeParameters()) classTypeParams.add(tp.getNameAsString());
      }
      if (t instanceof RecordDeclaration rd) {
        for (var tp : rd.getTypeParameters()) classTypeParams.add(tp.getNameAsString());
        // A METHOD-targeted annotation on a record component applies to the implicit accessor.
        for (var component : rd.getParameters()) {
          if (component.getAnnotations().stream()
              .noneMatch(a -> a.getNameAsString().equals("CodegenDescriptor"))) {
            continue;
          }
          out.add(
              new Entry(
                  owner,
                  screamingSnake(simple) + "_" + screamingSnake(component.getNameAsString()),
                  component.getNameAsString(),
                  typeNameOf(component.getType(), imports, pkg, classTypeParams),
                  TList.of(),
                  false,
                  false));
        }
      }
      for (var m : t.getMethods()) {
        if (!isAnnotated(m)) continue;
        var typeParams = new TSet<String>();
        typeParams.addAll(classTypeParams);
        for (var tp : m.getTypeParameters()) typeParams.add(tp.getNameAsString());
        var returnType = typeNameOf(m.getType(), imports, pkg, typeParams);
        var paramTypes = new TList<TypeName>();
        for (var p : m.getParameters()) {
          paramTypes.add(typeNameOf(p.getType(), imports, pkg, typeParams));
        }
        var constantName =
            screamingSnake(simple) + "_" + screamingSnake(m.getNameAsString());
        out.add(
            new Entry(
                owner,
                constantName,
                m.getNameAsString(),
                returnType,
                paramTypes,
                m.isStatic(),
                ownerIsInterface));
      }
    }
    for (var member : t.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        var nextEnclosing = enclosing.isEmpty() ? simple : enclosing + "." + simple;
        walkType(pkg, nextEnclosing, nested, imports, out);
      }
    }
  }

  private static boolean isAnnotated(MethodDeclaration m) {
    return m.getAnnotations().stream()
        .anyMatch(a -> a.getNameAsString().equals("CodegenDescriptor"));
  }

  private static TypeName typeNameOf(
      Type t, TMap<String, String> imports, String pkg, TSet<String> typeParams) {
    if (t.isVoidType()) return TypeName.VOID;
    if (t.isPrimitiveType()) {
      return switch (t.asPrimitiveType().asString()) {
        case "boolean" -> TypeName.BOOLEAN;
        case "byte" -> TypeName.BYTE;
        case "short" -> TypeName.SHORT;
        case "int" -> TypeName.INT;
        case "long" -> TypeName.LONG;
        case "char" -> TypeName.CHAR;
        case "float" -> TypeName.FLOAT;
        case "double" -> TypeName.DOUBLE;
        default -> throw new IllegalStateException("Unsupported primitive: " + t);
      };
    }
    if (t.isArrayType()) {
      return ArrayTypeName.of(
          typeNameOf(t.asArrayType().getComponentType(), imports, pkg, typeParams));
    }
    if (t.isClassOrInterfaceType()) {
      var name = t.asClassOrInterfaceType().getNameAsString();
      // Unbounded type variables erase to Object in the JVM descriptor.
      if (typeParams.contains(name)) return ClassName.get(Object.class);
      return ClassName.bestGuess(resolve(name, imports, pkg));
    }
    throw new IllegalStateException("Unsupported @CodegenDescriptor type: " + t);
  }

  private static final TList<String> JAVA_LANG =
      TList.of(
          "String", "Object", "Integer", "Long", "Short", "Byte", "Boolean", "Float", "Double",
          "Character", "CharSequence", "Number");

  private static String resolve(String name, TMap<String, String> imports, String pkg) {
    var head = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
    var fromImport = imports.get(head);
    if (fromImport != null) {
      return name.contains(".") ? fromImport + name.substring(name.indexOf('.')) : fromImport;
    }
    if (JAVA_LANG.contains(head)) return "java.lang." + name;
    return pkg.isEmpty() ? name : pkg + "." + name;
  }

  private static String screamingSnake(String camel) {
    var sb = new StringBuilder();
    for (int i = 0; i < camel.length(); i++) {
      var c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toUpperCase(c));
    }
    return sb.toString();
  }
}
