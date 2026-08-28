package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
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
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import compilerxyz.codegen.util.BuiltinMethodEmit.Method;
import compilerxyz.codegen.util.BuiltinMethodEmit.Param;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import compilerxyz.codegen.util.NameDerivation;
import java.util.HashSet;
import compilerxyz.util.IMap;
import compilerxyz.util.Pair;

public final class JavaAspectExtractor {
  private JavaAspectExtractor() {}

  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  public record ScanResult(TList<BuiltinInterfacesGen.BuiltinSpec> builtins) {}

  public static ScanResult scanRoot(Path runtimeSourceRoot) throws IOException {
    var builtins = new TList<BuiltinInterfacesGen.BuiltinSpec>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("@BuiltinFull(")) continue;
        var cu = StaticJavaParser.parse(file);
        var declOpt = findBuiltinType(cu);
        if (declOpt.optIsSome()) {
          var decl = declOpt.optUnwrap();
          Opt<BuiltinInterfacesGen.BuiltinSpec> builtin;
          if (decl instanceof ClassOrInterfaceDeclaration coid) {
            if (coid.isInterface()) {
              builtin = extractBuiltinSpecInterface(
                  file, cu, coid, interfaceExtendsAspect(coid));
            } else {
              builtin = extractBuiltinSpecClass(file, cu, coid);
            }
          } else if (decl instanceof RecordDeclaration recDecl) {
            builtin = extractBuiltinSpecRecord(file, cu, recDecl, Opt.none());
          } else {
            throw new IllegalStateException(
                "JavaAspectExtractor: unexpected decl type " + decl.getClass());
          }
          if (builtin.optIsSome()) builtins.add(builtin.optUnwrap());
        }
        // Also scan for nested @Builtin records inside any top-level type.
        for (var t : cu.getTypes()) {
          collectNestedBuiltinRecords(file, cu, t, builtins);
        }
      }
    }
    return new ScanResult(builtins);
  }

  private static void collectNestedBuiltinRecords(
      Path sourceFile,
      CompilationUnit cu,
      TypeDeclaration<?> outer,
      TList<BuiltinInterfacesGen.BuiltinSpec> builtins) {
    var outerSimple = outer.getNameAsString();
    for (var member : outer.getMembers()) {
      if (member instanceof RecordDeclaration rec) {
        var hasBuiltin = rec.getAnnotationByName("BuiltinFull").isPresent()
            || rec.getAnnotationByName("compilerxyz.annotations.BuiltinFull").isPresent();
        if (hasBuiltin) {
          var spec = extractBuiltinSpecRecord(sourceFile, cu, rec, Opt.some(outerSimple));
          if (spec.optIsSome()) builtins.add(spec.optUnwrap());
        }
      }
      if (member instanceof TypeDeclaration<?> nested) {
        collectNestedBuiltinRecords(sourceFile, cu, nested, builtins);
      }
    }
  }

  private static Opt<BuiltinInterfacesGen.BuiltinSpec> extractBuiltinSpecInterface(
      Path sourceFile,
      CompilationUnit cu,
      ClassOrInterfaceDeclaration ifaceDecl,
      boolean isAspect) {
    var simpleName = ifaceDecl.getNameAsString();
    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var builtinAnn = getBuiltinAnnotation(ifaceDecl);
    var attrs = readAttrs(builtinAnn);
    var uniqueIdStr = attrs.get("uniqueId");
    if (uniqueIdStr == null) {
      throw new IllegalStateException("@Builtin on " + simpleName + " missing required uniqueId");
    }
    var uniqueId = Long.parseLong(uniqueIdStr);
    var identityIdStr = attrs.get("identityId");
    var identityId = identityIdStr == null ? 0L : Long.parseLong(identityIdStr);
    if (isAspect && identityId == 0L) {
      throw new IllegalStateException(
          "@Builtin on aspect " + simpleName + " missing required identityId");
    }
    var importMap = buildImportMap(cu);
    // Nested records declared inside an aspect interface become
    // compilerxyz.gen.AspectXxx.NestedName in generated code; wire their short
    // names so method signatures referencing them resolve correctly.
    var effectiveImports = new TMap<String, String>();
    for (var e : importMap.entrySet()) effectiveImports.put(e.getKey(), e.getValue());
    if (isAspect) {
      for (var member : ifaceDecl.getMembers()) {
        if (member instanceof RecordDeclaration r) {
          effectiveImports.put(
              r.getNameAsString(), pkg + "." + simpleName + "." + r.getNameAsString());
        }
      }
    }
    var methods = new TList<Method>();
    for (var m : ifaceDecl.getMethods()) {
      if (!m.isAbstract()) continue;
      var sourceName = NameDerivation.snakeCase(
          deriveSourceName(m.getNameAsString(), simpleName));
      var params = new TList<Param>();
      for (var p : m.getParameters()) {
        params.add(new Param(
            NameDerivation.snakeCase(stripReservedSuffix(p.getNameAsString())),
            typeNameOf(p.getType(), effectiveImports, pkg)));
      }
      methods.add(new Method(
          sourceName,
          m.getNameAsString(),
          params,
          typeNameOf(m.getType(), effectiveImports, pkg),
          true));
    }
    return Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
        simpleName, pkg, uniqueId, identityId, isAspect, false, methods,
        new TList<BuiltinInterfacesGen.DataField>()));
  }

  private static Opt<BuiltinInterfacesGen.BuiltinSpec> extractBuiltinSpecClass(
      Path sourceFile, CompilationUnit cu, ClassOrInterfaceDeclaration clsDecl) {
    var simpleName = clsDecl.getNameAsString();
    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var builtinAnn = getBuiltinAnnotation(clsDecl);
    var attrs = readAttrs(builtinAnn);
    var uniqueIdStr = attrs.get("uniqueId");
    if (uniqueIdStr == null) {
      throw new IllegalStateException("@Builtin on " + simpleName + " missing required uniqueId");
    }
    var uniqueId = Long.parseLong(uniqueIdStr);
    return Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
        simpleName, pkg, uniqueId, 0L, false, false,
        new TList<Method>(),
        new TList<BuiltinInterfacesGen.DataField>()));
  }

  private static Opt<BuiltinInterfacesGen.BuiltinSpec> extractBuiltinSpecRecord(
      Path sourceFile,
      CompilationUnit cu,
      RecordDeclaration recDecl,
      Opt<String> outerSimpleName) {
    var nestedName = recDecl.getNameAsString();
    var simpleName = outerSimpleName.optIsNone() ? nestedName : outerSimpleName.optUnwrap() + nestedName;
    var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
    var builtinAnn = getBuiltinAnnotation(recDecl);
    var attrs = readAttrs(builtinAnn);
    var uniqueIdStr = attrs.get("uniqueId");
    if (uniqueIdStr == null) {
      throw new IllegalStateException("@Builtin on " + simpleName + " missing required uniqueId");
    }
    var uniqueId = Long.parseLong(uniqueIdStr);
    var importMap = buildImportMap(cu);
    var dataFields = new TList<BuiltinInterfacesGen.DataField>();
    var methods = new TList<Method>();
    var recordComponentNames = new HashSet<String>();
    for (var comp : recDecl.getParameters()) {
      var compName = comp.getNameAsString();
      recordComponentNames.add(compName);
      dataFields.add(new BuiltinInterfacesGen.DataField(
          NameDerivation.snakeCase(compName),
          compName,
          typeNameOf(comp.getType(), importMap, pkg)));
    }
    for (var m : recDecl.getMethods()) {
      if (m.isStatic()) continue;
      if (!m.getParameters().isEmpty()) continue;
      if (recordComponentNames.contains(m.getNameAsString())) continue;
      var sourceName = NameDerivation.snakeCase(m.getNameAsString());
      methods.add(new Method(
          sourceName,
          m.getNameAsString(),
          new TList<Param>(),
          typeNameOf(m.getType(), importMap, pkg),
          true));
    }
    return Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
        simpleName, pkg, uniqueId, 0L, false, true, methods, dataFields,
        outerSimpleName, outerSimpleName.optIsNone() ? Opt.none() : Opt.some(nestedName)));
  }

  private static Opt<TypeDeclaration<?>> findBuiltinType(CompilationUnit cu) {
    for (var t : cu.getTypes()) {
      var hasBuiltin = t.getAnnotationByName("BuiltinFull").isPresent()
          || t.getAnnotationByName("compilerxyz.annotations.BuiltinFull").isPresent();
      if (!hasBuiltin) continue;
      if (t instanceof ClassOrInterfaceDeclaration c) return Opt.some(c);
      if (t instanceof RecordDeclaration r) return Opt.some(r);
    }
    return Opt.none();
  }

  private static boolean interfaceExtendsAspect(ClassOrInterfaceDeclaration decl) {
    for (var ext : decl.getExtendedTypes()) {
      var n = ext.getNameAsString();
      if (n.equals("Aspect") || n.equals("compilerxyz.eval.aspect.Aspect")) return true;
    }
    return false;
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

  private static final IMap<String, ClassName> COMPOSITE_WRAPPERS =
      IMap.ofEntries(
      new Pair<>("compilerxyz.eval.EResult",
          ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic")),
      new Pair<>("compilerxyz.util.Opt",
          ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic")),
      new Pair<>("compilerxyz.util.TList",
          ClassName.get("compilerxyz.eval.value.list", "TypeListBoxedDynamic")),
      new Pair<>("compilerxyz.util.TMap",
          ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic")));

  private static final IMap<String, ClassName> VALUE_CLASS_DESCRIPTORS =
      IMap.ofEntries(
      new Pair<>("java.lang.String",
          ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic")),
      new Pair<>("compilerxyz.eval.Unique",
          ClassName.get("compilerxyz.eval.value.unique", "TypeUniqueDynamic")));

  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_INT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.int_", "TypeIntDynamic");

  private static AnnotationExpr getBuiltinAnnotation(TypeDeclaration<?> decl) {
    return decl.getAnnotationByName("BuiltinFull")
        .or(() -> decl.getAnnotationByName("compilerxyz.annotations.BuiltinFull"))
        .orElseThrow(() -> new IllegalStateException(
            "@BuiltinFull missing on " + decl.getNameAsString()));
  }

  private static AnnotationExpr requireAnnotation(
      NodeWithAnnotations<?> node, String name) {
    return node.getAnnotationByName(name)
        .orElseThrow(() -> new IllegalStateException("Missing @" + name));
  }

  private static long readSingleLongAttr(AnnotationExpr annot) {
    if (annot instanceof SingleMemberAnnotationExpr sm) {
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
