package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
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
import compilerxyz.codegen.BuiltinInterfacesGen;
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
      TList<BuiltinInterfacesGen.BuiltinSpec> builtins,
      TList<InterfaceSpec> sourceInterfaces) {}

  public static ScanResult scanRoot(Path runtimeSourceRoot) throws IOException {
    var builtins = new TList<BuiltinInterfacesGen.BuiltinSpec>();
    var interfaces = new TList<InterfaceSpec>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("@Builtin(")) continue;
        var cu = StaticJavaParser.parse(file);
        var decl = findBuiltinInterface(cu);
        if (decl == null) continue;
        var isAspect = interfaceExtendsAspect(decl);
        var builtin = extractBuiltinSpec(file, cu, decl, isAspect);
        if (builtin != null) builtins.add(builtin);
        if (isAspect) {
          var iface = extractSourceInterface(file);
          if (iface != null) interfaces.add(iface);
        } else {
          var trait = extractTrait(file);
          if (trait != null) interfaces.add(trait);
        }
      }
    }
    return new ScanResult(builtins, interfaces);
  }

  private static BuiltinInterfacesGen.@org.jspecify.annotations.Nullable BuiltinSpec
      extractBuiltinSpec(
          Path sourceFile,
          CompilationUnit cu,
          ClassOrInterfaceDeclaration ifaceDecl,
          boolean isAspect) {
    var simpleName = ifaceDecl.getNameAsString();
    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var typeIdAnn = ifaceDecl.getAnnotationByName("BuiltinTypeId")
        .orElseThrow(() -> new IllegalStateException(
            "@Builtin interface " + simpleName + " missing required @BuiltinTypeId"));
    var typeId = readSingleLongAttr(typeIdAnn);
    var builtinAnn = getBuiltinAnnotation(ifaceDecl);
    var attrs = readAttrs(builtinAnn);
    var uniqueIdStr = attrs.get("uniqueId");
    if (uniqueIdStr == null) {
      throw new IllegalStateException("@Builtin on " + simpleName + " missing required uniqueId");
    }
    var uniqueId = Long.parseLong(uniqueIdStr);
    Long trivialIdentity = null;
    var tiaOpt = ifaceDecl.getAnnotationByName("BuiltinTrivialIdentityAspect");
    if (tiaOpt.isPresent()) {
      trivialIdentity = readSingleLongAttr(tiaOpt.get());
    }
    var importMap = buildImportMap(cu);
    // Nested records declared inside an aspect interface become
    // compilerxyz.gen.AspectXxx.NestedName in generated code; wire their short
    // names so method signatures referencing them resolve correctly.
    var effectiveImports = new TMap<String, String>();
    for (var e : importMap.entrySet()) effectiveImports.put(e.getKey(), e.getValue());
    if (isAspect) {
      for (var member : ifaceDecl.getMembers()) {
        if (member instanceof com.github.javaparser.ast.body.RecordDeclaration r) {
          effectiveImports.put(
              r.getNameAsString(), pkg + "." + simpleName + "." + r.getNameAsString());
        }
      }
    }
    var methods = new TList<compilerxyz.codegen.util.BuiltinMethodEmit.Method>();
    for (var m : ifaceDecl.getMethods()) {
      // Aspects emit all abstract methods (so their TOD advertises AspectField
      // via method count); traits emit only @SourceExposed methods.
      if (!m.isAbstract()) continue;
      if (!isAspect && m.getAnnotationByName("SourceExposed").isEmpty()) continue;
      var sourceName = compilerxyz.codegen.util.NameDerivation.snakeCase(
          deriveSourceName(m.getNameAsString(), simpleName));
      var params = new TList<compilerxyz.codegen.util.BuiltinMethodEmit.Param>();
      for (var p : m.getParameters()) {
        params.add(new compilerxyz.codegen.util.BuiltinMethodEmit.Param(
            compilerxyz.codegen.util.NameDerivation.snakeCase(stripReservedSuffix(p.getNameAsString())),
            typeNameOf(p.getType(), effectiveImports, pkg)));
      }
      methods.add(new compilerxyz.codegen.util.BuiltinMethodEmit.Method(
          sourceName,
          m.getNameAsString(),
          params,
          typeNameOf(m.getType(), effectiveImports, pkg),
          true));
    }
    return new BuiltinInterfacesGen.BuiltinSpec(
        simpleName, pkg, typeId, uniqueId, isAspect, trivialIdentity, methods);
  }

  private static @org.jspecify.annotations.Nullable ClassOrInterfaceDeclaration
      findBuiltinInterface(CompilationUnit cu) {
    for (var t : cu.getTypes()) {
      if (t instanceof ClassOrInterfaceDeclaration c
          && c.isInterface()
          && (c.getAnnotationByName("Builtin").isPresent()
              || c.getAnnotationByName("compilerxyz.annotations.Builtin").isPresent())) {
        return c;
      }
    }
    return null;
  }

  private static boolean interfaceExtendsAspect(ClassOrInterfaceDeclaration decl) {
    for (var ext : decl.getExtendedTypes()) {
      var n = ext.getNameAsString();
      if (n.equals("Aspect") || n.equals("compilerxyz.eval.aspect.Aspect")) return true;
    }
    return false;
  }

  private static @org.jspecify.annotations.Nullable InterfaceSpec extractTrait(Path sourceFile)
      throws IOException {
    var cu = StaticJavaParser.parse(sourceFile);
    var ifaceDecl = findBuiltinInterface(cu);
    if (ifaceDecl == null) return null;
    var traitAnn = getBuiltinAnnotation(ifaceDecl);
    var attrs = readAttrs(traitAnn);
    var importMap = buildImportMap(cu);
    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var simpleName = ifaceDecl.getNameAsString();
    var name = snakeCase(simpleName);

    var methods = new TList<Method>();
    for (var m : ifaceDecl.getMethods()) {
      var exposedOpt = m.getAnnotationByName("SourceExposed");
      if (exposedOpt.isEmpty()) continue;
      var derivedName = deriveSourceName(m.getNameAsString(), simpleName);
      var args = new TList<NamedTypeRef>();
      for (var p : m.getParameters()) {
        args.add(new NamedTypeRef(
            snakeCase(stripReservedSuffix(p.getNameAsString())),
            typeRefOf(p.getType(), importMap, pkg)));
      }
      methods.add(new Method(derivedName, typeRefOf(m.getType(), importMap, pkg),
          new MethodArgs(args)));
    }

    var registerInScope = !"false".equals(attrs.getOrDefault("registerInScope", "true"));
    return new InterfaceSpec(
        name,
        Integer.parseInt(attrs.get("uniqueId")),
        Integer.parseInt(attrs.get("scopeId")),
        simpleName,
        null,
        registerInScope,
        null,
        pkg + "." + simpleName,
        methods);
  }

  /**
   * Derive the source-visible method name from the Java method name and the enclosing
   * interface name. If the Java name starts with the interface name (lowercase first char),
   * strip that prefix and lowercase the first remaining char. Otherwise return the Java
   * name unchanged.
   */
  private static String deriveSourceName(String javaMethodName, String interfaceSimpleName) {
    var prefix = Character.toLowerCase(interfaceSimpleName.charAt(0)) + interfaceSimpleName.substring(1);
    if (javaMethodName.startsWith(prefix) && javaMethodName.length() > prefix.length()) {
      var rest = javaMethodName.substring(prefix.length());
      return Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
    }
    return javaMethodName;
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
    var ifaceDecl = findBuiltinInterface(cu);
    if (ifaceDecl == null) return null;
    var siAnnOpt = ifaceDecl.getAnnotationByName("SourceInterface");
    if (siAnnOpt.isEmpty()) return null;
    var siAttrs = readAttrs(siAnnOpt.get());
    var simpleName = ifaceDecl.getNameAsString();
    if (!simpleName.startsWith("Aspect")) {
      throw new IllegalStateException(
          "@Builtin aspect interface must be named AspectXxx: " + simpleName);
    }
    var kind = simpleName.substring("Aspect".length());
    var aspectPrefix = "aspect" + kind;
    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var importMap = buildImportMap(cu);
    var name = "aspect_" + camelToSnake(kind);

    var methods = new TList<Method>();
    for (var m : ifaceDecl.getMethods()) {
      var exposedOpt = m.getAnnotationByName("SourceExposed");
      if (exposedOpt.isEmpty()) continue;
      var sourceName = deriveSourceName(m.getNameAsString(), "Aspect" + kind);
      var args = new TList<NamedTypeRef>();
      for (var p : m.getParameters()) {
        args.add(new NamedTypeRef(
            snakeCase(stripReservedSuffix(p.getNameAsString())),
            typeRefOf(p.getType(), importMap, pkg)));
      }
      methods.add(new Method(sourceName, typeRefOf(m.getType(), importMap, pkg), new MethodArgs(args)));
    }

    return new InterfaceSpec(
        name,
        Integer.parseInt(siAttrs.get("uniqueId")),
        Integer.parseInt(siAttrs.get("scopeId")),
        "Aspect" + kind,
        null,
        true,
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

  private static TypeRef typeRefOf(Type t, TMap<String, String> imports, String sourcePkg) {
    var tn = typeNameOf(t, imports, sourcePkg);
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

  private static AnnotationExpr getBuiltinAnnotation(ClassOrInterfaceDeclaration decl) {
    return decl.getAnnotationByName("Builtin")
        .or(() -> decl.getAnnotationByName("compilerxyz.annotations.Builtin"))
        .orElseThrow(() -> new IllegalStateException(
            "@Builtin missing on " + decl.getNameAsString()));
  }

  private static AnnotationExpr requireAnnotation(
      com.github.javaparser.ast.nodeTypes.NodeWithAnnotations<?> node, String name) {
    return node.getAnnotationByName(name)
        .orElseThrow(() -> new IllegalStateException("Missing @" + name));
  }

  private static long readSingleLongAttr(AnnotationExpr annot) {
    if (annot instanceof com.github.javaparser.ast.expr.SingleMemberAnnotationExpr sm) {
      var raw = sm.getMemberValue().toString();
      if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
      return Long.parseLong(raw);
    }
    if (annot instanceof NormalAnnotationExpr n) {
      for (var p : n.getPairs()) {
        if (p.getNameAsString().equals("value")) {
          var raw = p.getValue().toString();
          if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
          return Long.parseLong(raw);
        }
      }
    }
    throw new IllegalStateException(
        "readSingleLongAttr: no value found in @" + annot.getNameAsString());
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

  private static TypeName typeNameOf(Type t, TMap<String, String> imports, String sourcePkg) {
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
      var raw = classNameOf(c.getNameAsString(), imports, sourcePkg);
      if (c.getTypeArguments().isPresent() && !c.getTypeArguments().get().isEmpty()) {
        var argsList = c.getTypeArguments().get();
        var argNames = new TypeName[argsList.size()];
        for (int i = 0; i < argsList.size(); i++) {
          argNames[i] = typeNameOf(argsList.get(i), imports, sourcePkg);
        }
        return ParameterizedTypeName.get(raw, argNames);
      }
      return raw;
    }
    throw new IllegalStateException("Unhandled type: " + t + " (" + t.getClass() + ")");
  }

  private static ClassName classNameOf(String simple, TMap<String, String> imports, String sourcePkg) {
    var fqn = imports.getOrNull(simple);
    if (fqn == null) {
      try {
        fqn = Class.forName("java.lang." + simple).getName();
      } catch (ClassNotFoundException e) {
        fqn = sourcePkg + "." + simple;
      }
    }
    var idx = fqn.lastIndexOf('.');
    return ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
  }
}
