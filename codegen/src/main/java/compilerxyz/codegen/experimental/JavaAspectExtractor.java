package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
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
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.Method;
import compilerxyz.codegen.util.MethodArgs;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
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

  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  public record ScanResult(
      TList<AspectsGen.AspectSpec> aspects, TList<InterfaceSpec> sourceInterfaces) {}

  public static ScanResult scanRoot(Path runtimeSourceRoot) throws IOException {
    var aspects = new TList<AspectsGen.AspectSpec>();
    var interfaces = new TList<InterfaceSpec>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        boolean hasAspect = src.contains("@BuiltinAspect");
        boolean hasTrait = src.contains("BuiltinTrait");
        if (!hasAspect && !hasTrait) continue;
        if (hasAspect) {
          var spec = extractOne(file);
          if (spec != null) aspects.add(spec);
          var iface = extractSourceInterface(file);
          if (iface != null) interfaces.add(iface);
        }
        if (hasTrait) {
          var trait = extractTrait(file);
          if (trait != null) interfaces.add(trait);
        }
      }
    }
    return new ScanResult(aspects, interfaces);
  }

  private static @org.jspecify.annotations.Nullable InterfaceSpec extractTrait(Path sourceFile)
      throws IOException {
    var cu = StaticJavaParser.parse(sourceFile);
    ClassOrInterfaceDeclaration ifaceDecl = null;
    for (var t : cu.getTypes()) {
      if (t instanceof ClassOrInterfaceDeclaration c && c.isInterface()) {
        for (var a : c.getAnnotations()) {
          var n = a.getNameAsString();
          if (n.equals("BuiltinTrait") || n.equals("compilerxyz.annotations.BuiltinTrait")) {
            ifaceDecl = c;
            break;
          }
        }
      }
      if (ifaceDecl != null) break;
    }
    if (ifaceDecl == null) return null;
    var traitAnn = ifaceDecl.getAnnotations().stream()
        .filter(a -> a.getNameAsString().equals("BuiltinTrait")
            || a.getNameAsString().equals("compilerxyz.annotations.BuiltinTrait"))
        .findFirst().orElseThrow();
    var attrs = readAttrs(traitAnn);
    var importMap = buildImportMap(cu);
    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var simpleName = ifaceDecl.getNameAsString();
    var name = snakeCase(simpleName);

    var methods = new TList<Method>();
    for (var m : ifaceDecl.getMethods()) {
      var exposedOpt = m.getAnnotationByName("SourceExposed");
      if (exposedOpt.isEmpty()) continue;
      var exposedAttrs = readAttrs(exposedOpt.get());
      var args = new TList<NamedTypeRef>();
      for (var p : m.getParameters()) {
        args.add(new NamedTypeRef(
            snakeCase(stripReservedSuffix(p.getNameAsString())),
            typeRefOf(p.getType(), importMap)));
      }
      methods.add(new Method(exposedAttrs.get("name"), typeRefOf(m.getType(), importMap),
          new MethodArgs(args)));
    }

    var typeIdConst = attrs.getOrDefault("typeIdConstant", "");
    var registerInScope = !"false".equals(attrs.getOrDefault("registerInScope", "true"));
    return new InterfaceSpec(
        name,
        Integer.parseInt(attrs.get("uniqueId")),
        Integer.parseInt(attrs.get("scopeId")),
        attrs.get("fieldPrefix"),
        simpleName,
        null,
        registerInScope,
        null,
        typeIdConst.isEmpty() ? null : typeIdConst,
        pkg + "." + simpleName,
        methods);
  }

  private static String stripReservedSuffix(String name) {
    if (name.endsWith("_") && name.length() > 1) return name.substring(0, name.length() - 1);
    return name;
  }

  private static String snakeCase(String s) {
    var sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      var c = s.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toLowerCase(c));
    }
    return sb.toString();
  }

  private static @org.jspecify.annotations.Nullable InterfaceSpec extractSourceInterface(
      Path sourceFile) throws IOException {
    var cu = StaticJavaParser.parse(sourceFile);
    var ifaceDecl = findAnnotatedInterface(cu);
    if (ifaceDecl == null) return null;
    var siAnnOpt = ifaceDecl.getAnnotationByName("SourceInterface");
    if (siAnnOpt.isEmpty()) return null;
    var siAttrs = readAttrs(siAnnOpt.get());
    var kindAttrs = readAttrs(requireAnnotation(ifaceDecl, "BuiltinAspect"));
    var kind = kindAttrs.get("kind");
    var aspectPrefix = "aspect" + kind;
    var importMap = buildImportMap(cu);
    var name = "aspect_" + camelToSnake(kind);

    var methods = new TList<Method>();
    for (var m : ifaceDecl.getMethods()) {
      var exposedOpt = m.getAnnotationByName("SourceExposed");
      if (exposedOpt.isEmpty()) continue;
      var exposedAttrs = readAttrs(exposedOpt.get());
      var sourceName = exposedAttrs.get("name");
      var args = new TList<NamedTypeRef>();
      for (var p : m.getParameters()) {
        args.add(new NamedTypeRef(
            snakeCase(stripReservedSuffix(p.getNameAsString())),
            typeRefOf(p.getType(), importMap)));
      }
      methods.add(new Method(sourceName, typeRefOf(m.getType(), importMap), new MethodArgs(args)));
    }

    return new InterfaceSpec(
        name,
        Integer.parseInt(siAttrs.get("uniqueId")),
        Integer.parseInt(siAttrs.get("scopeId")),
        siAttrs.get("fieldPrefix"),
        "Aspect" + kind,
        null,
        true,
        null,
        null,
        "compilerxyz.gen.Aspect" + kind,
        methods);
  }

  private static final Map<String, ClassName> COMPOSITE_WRAPPERS = Map.of(
      "compilerxyz.eval.EResult",
          ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic"),
      "compilerxyz.util.Opt",
          ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic"),
      "compilerxyz.util.TList",
          ClassName.get("compilerxyz.eval.value.list", "TypeListDynamic"),
      "compilerxyz.util.TMap",
          ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic"));

  private static final Map<String, ClassName> VALUE_CLASS_DESCRIPTORS = Map.of(
      "java.lang.String",
          ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic"),
      "compilerxyz.eval.Unique",
          ClassName.get("compilerxyz.eval.value.unique", "TypeUnique"));

  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_NUMBER_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.number", "TypeNumberDynamic");

  private static TypeRef typeRefOf(Type t, TMap<String, String> imports) {
    var tn = typeNameOf(t, imports);
    if (tn.equals(TypeName.BOOLEAN)) return TypeRef.leaf(TYPE_BOOL_DYNAMIC);
    if (tn.equals(TypeName.DOUBLE)) return TypeRef.leaf(TYPE_NUMBER_DYNAMIC);
    if (tn instanceof ClassName cn) {
      var descriptor = VALUE_CLASS_DESCRIPTORS.get(cn.canonicalName());
      if (descriptor != null) return TypeRef.leaf(descriptor);
      try {
        return TypeRef.leaf(cn);
      } catch (IllegalArgumentException e) {
        // Not in DynamicTypes' leaf table — fall through to opaque.
      }
    }
    if (tn instanceof ParameterizedTypeName pt) {
      var wrapper = COMPOSITE_WRAPPERS.get(pt.rawType.canonicalName());
      if (wrapper != null && pt.typeArguments.size() == 1
          && pt.typeArguments.get(0) instanceof ClassName inner) {
        return TypeRef.composite1(wrapper, TypeRef.leaf(inner));
      }
    }
    return TypeRef.opaque(tn);
  }

  private static String camelToSnake(String camel) {
    var sb = new StringBuilder();
    for (int i = 0; i < camel.length(); i++) {
      var c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toLowerCase(c));
    }
    return sb.toString();
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
    var aspectClassName = ClassName.get("compilerxyz.gen", "Aspect" + kind);

    // Nested records declared inside the interface become AspectSpec.nestedRecords.
    var nestedRecords = new TList<AspectsGen.NestedRecord>();
    var nestedRecordNames = new java.util.HashSet<String>();
    for (var member : ifaceDecl.getMembers()) {
      if (member instanceof RecordDeclaration r) {
        var components = new TList<AspectsGen.Param>();
        for (var p : r.getParameters()) {
          var nullable = p.getAnnotationByName("Nullable").isPresent();
          components.add(
              new AspectsGen.Param(
                  p.getNameAsString(), typeNameOf(p.getType(), importMap), nullable));
        }
        nestedRecords.add(new AspectsGen.NestedRecord("", r.getNameAsString(), components));
        nestedRecordNames.add(r.getNameAsString());
      }
    }
    // Wire nested-record short names to their generated FQNs (compilerxyz.gen.AspectXxx.Name)
    // so method signatures referencing them resolve correctly.
    var effectiveImports = new TMap<String, String>();
    for (var e : importMap.entrySet()) effectiveImports.put(e.getKey(), e.getValue());
    for (var nr : nestedRecordNames) {
      effectiveImports.put(nr, aspectClassName.canonicalName() + "." + nr);
    }

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
                p.getNameAsString(), typeNameOf(p.getType(), effectiveImports), nullable));
      }
      methods.add(
          new AspectsGen.AspectMethod(
              "", shortName, params, typeNameOf(m.getType(), effectiveImports)));
    }

    return new AspectsGen.AspectSpec(
        javadoc, kind, Long.parseLong(attrs.get("uniqueId")), nestedRecords, methods);
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
    if (fqn == null) {
      // Fall back to well-known package for same-package references in handwritten runtime code.
      // Types in compilerxyz.eval, compilerxyz.gen, compilerxyz.util rarely need explicit imports
      // when the reference is in the same package. Try each.
      for (var pkg : new String[]{"compilerxyz.eval", "compilerxyz.gen", "compilerxyz.util"}) {
        try {
          var cls = Class.forName(pkg + "." + simple);
          fqn = cls.getName();
          break;
        } catch (ClassNotFoundException e) {
          // continue
        }
      }
      if (fqn == null) fqn = "java.lang." + simple;
    }
    var idx = fqn.lastIndexOf('.');
    return ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
  }
}
