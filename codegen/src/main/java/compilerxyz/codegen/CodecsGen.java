package compilerxyz.codegen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public final class CodecsGen {
  private CodecsGen() {}

  static final String GEN_PACKAGE = "compilerxyz.gen.codec";

  private static final TSet<String> HAND_CODED =
      TSet.of(
          "compilerxyz.safejson.JsonValue",
          "compilerxyz.artifact.Artifact",
          "compilerxyz.artifact.ArtifactBody",
          "compilerxyz.artifact.ArtifactBodyExporter",
          "compilerxyz.artifact.ArtifactIdSource",
          "compilerxyz.util.Opt",
          "compilerxyz.util.Opt.Some",
          "compilerxyz.util.Opt.None");

  private static final String MARKER = "compilerxyz.safejson.SafeJsonSerializable";

  static void run(Path outRoot, Formatter formatter, Path runtimeSourceRoot) throws IOException {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    var index = new TypeIndex();
    index.scan(runtimeSourceRoot);

    var roots = new TList<String>();
    for (var entry : index.byQname.entrySet()) {
      if (entry.getValue().implementsMarker) {
        roots.add(entry.getKey());
      }
    }
    var sortedRoots = roots.sortedBy(String::compareTo);

    var closure = new TOrderedSet<String>();
    for (var root : sortedRoots) {
      walk(root, index, closure);
    }

    var emitted = new TList<String>();
    for (var qname : closure) {
      if (HAND_CODED.contains(qname)) continue;
      var t = index.byQname.get(qname);
      if (t == null) continue;
      if (t.kind == TypeKind.RECORD || t.kind == TypeKind.SEALED || t.kind == TypeKind.ENUM) {
        var src = CodecEmitter.emit(qname, t, index);
        writeFormatted(outRoot, formatter, qname, src);
        emitted.add(qname);
      }
    }

    var initSrc = CodecEmitter.emitInit(emitted, closure, index, HAND_CODED);
    writeFormattedRaw(outRoot, formatter, GEN_PACKAGE + ".CodecsInit", initSrc);
  }

  private static void walk(String qname, TypeIndex index, TOrderedSet<String> seen) {
    if (!seen.add(qname)) return;
    if (HAND_CODED.contains(qname)) return;
    var t = index.byQname.get(qname);
    if (t == null) return;
    if (t.kind == TypeKind.SEALED) {
      for (var perm : t.permittedSubclasses) {
        walk(perm, index, seen);
      }
    } else if (t.kind == TypeKind.RECORD) {
      for (var comp : t.components) {
        for (var dep : referencedTypeNames(comp.type, index, qname)) {
          if (dep != null) walk(dep, index, seen);
        }
      }
    }
  }

  static TList<@Nullable String> referencedTypeNames(
      ParsedType type, TypeIndex index, String contextQname) {
    var out = new TList<@Nullable String>();
    if (type.kind == ParsedTypeKind.NAMED) {
      var resolved = index.resolve(type.name, contextQname);
      if (resolved != null) out.add(resolved);
      for (var ta : type.typeArgs) {
        out.addAll(referencedTypeNames(ta, index, contextQname));
      }
    }
    return out;
  }

  private static void writeFormatted(
      Path outRoot, Formatter formatter, String qname, String source) throws IOException {
    var className = CodecEmitter.simpleNameOf(qname) + "Codec";
    var fqcn = GEN_PACKAGE + "." + className;
    writeFormattedRaw(outRoot, formatter, fqcn, source);
  }

  private static void writeFormattedRaw(
      Path outRoot, Formatter formatter, String fqcn, String source) throws IOException {
    String formatted;
    try {
      formatted = formatter.formatSource(source);
    } catch (FormatterException e) {
      throw new IOException("palantir-java-format failed for " + fqcn + "\n--- source ---\n" + source, e);
    }
    var slash = fqcn.lastIndexOf('.');
    var pkg = fqcn.substring(0, slash);
    var simple = fqcn.substring(slash + 1);
    var pkgDir = outRoot.resolve(pkg.replace('.', '/'));
    Files.createDirectories(pkgDir);
    var path = pkgDir.resolve(simple + ".java");
    Files.writeString(
        path, formatted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  enum TypeKind {
    RECORD,
    SEALED,
    ENUM,
    OTHER
  }

  enum ParsedTypeKind {
    NAMED,
    PRIMITIVE,
    BYTE_ARRAY
  }

  static final class ParsedType {
    final ParsedTypeKind kind;
    String name;
    final TList<ParsedType> typeArgs;
    final boolean nullable;

    ParsedType(ParsedTypeKind kind, String name, TList<ParsedType> typeArgs, boolean nullable) {
      this.kind = kind;
      this.name = name;
      this.typeArgs = typeArgs;
      this.nullable = nullable;
    }
  }

  static final class Component {
    final String name;
    final ParsedType type;

    Component(String name, ParsedType type) {
      this.name = name;
      this.type = type;
    }
  }

  static final class TypeInfo {
    TypeKind kind = TypeKind.OTHER;
    boolean implementsMarker;
    final TList<Component> components = new TList<>();
    final TList<String> permittedSubclasses = new TList<>();
    final TList<String> enumConstants = new TList<>();

    final TList<String> implementedSimpleNames = new TList<>();

    final TMap<String, String> imports = new TMap<>();

    final String pkg = "";
    String packageName = "";

    @Override
    public String toString() {
      return kind + " marker=" + implementsMarker + " comps=" + components.size();
    }
  }

  static final class TypeIndex {

    final TOrderedMap<String, TypeInfo> byQname = new TOrderedMap<>();

    final TMap<String, TList<String>> byPackage = new TMap<>();

    void scan(Path runtimeSourceRoot) throws IOException {
      try (Stream<Path> walk = Files.walk(runtimeSourceRoot)) {
        var files = walk.filter(p -> p.toString().endsWith(".java")).toList();
        for (var p : files) {
          parseFile(p);
        }
      }

    }

    private void parseFile(Path file) throws IOException {
      CompilationUnit cu;
      try {
        cu = StaticJavaParser.parse(file);
      } catch (Exception e) {
        throw new IOException("JavaParser failed on " + file, e);
      }
      var pkgDecl = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

      var importMap = new TMap<String, String>();
      for (var imp : cu.getImports()) {
        if (imp.isAsterisk() || imp.isStatic()) continue;
        var full = imp.getNameAsString();
        var dot = full.lastIndexOf('.');
        var simple = dot < 0 ? full : full.substring(dot + 1);
        importMap.put(simple, full);
      }
      var cuRegistered = new TList<String>();
      for (var td : cu.getTypes()) {
        registerType(td, pkgDecl, pkgDecl, importMap, cuRegistered);
      }

      for (var sealedQname : cuRegistered) {
        var sealedInfo = byQname.get(sealedQname);
        if (sealedInfo == null || sealedInfo.kind != TypeKind.SEALED) continue;
        if (!sealedInfo.permittedSubclasses.isEmpty()) continue;
        var sealedSimple = sealedQname.substring(sealedQname.lastIndexOf('.') + 1);
        for (var otherQname : cuRegistered) {
          if (otherQname.equals(sealedQname)) continue;
          var other = byQname.get(otherQname);
          if (other == null) continue;
          if (other.implementedSimpleNames.contains(sealedSimple)) {
            sealedInfo.permittedSubclasses.add(otherQname);
          }
        }
      }
    }

    private void registerType(
        TypeDeclaration<?> td,
        String pkg,
        String enclosingQname,
        TMap<String, String> importMap,
        TList<String> cuRegistered) {
      var simple = td.getNameAsString();
      var qname = enclosingQname.isEmpty() ? simple : enclosingQname + "." + simple;

      var info = new TypeInfo();
      info.packageName = pkg;
      info.imports.putAll(importMap);

      if (td instanceof RecordDeclaration rd) {
        info.kind = TypeKind.RECORD;
        for (var param : rd.getParameters()) {
          var nullable = param.getAnnotations().stream()
                  .anyMatch(a -> a.getNameAsString().equals("Nullable"))
              || param.getType().getAnnotations().stream()
                  .anyMatch(a -> a.getNameAsString().equals("Nullable"));
          var pt = parseType(param.getType(), nullable);
          info.components.add(new Component(param.getNameAsString(), pt));
        }
        for (var iface : rd.getImplementedTypes()) {
          info.implementedSimpleNames.add(iface.getNameAsString());
        }
        info.implementsMarker = checkMarker(rd, importMap, pkg);
      } else if (td instanceof ClassOrInterfaceDeclaration cid) {
        if (cid.isInterface() && cid.getModifiers().stream()
            .anyMatch(m -> m.getKeyword().asString().equals("sealed"))) {
          info.kind = TypeKind.SEALED;
          for (var pt : cid.getPermittedTypes()) {
            var qn = resolveSimpleName(pt.getNameAsString(), importMap, pkg);
            info.permittedSubclasses.add(qn);
          }

          if (info.permittedSubclasses.isEmpty()) {
            for (var member : cid.getMembers()) {
              if (member instanceof TypeDeclaration<?> nested) {
                var implementedBy = isImplementedBy(nested, cid.getNameAsString());
                if (implementedBy) {
                  info.permittedSubclasses.add(qname + "." + nested.getNameAsString());
                }
              }
            }
          }
          info.implementsMarker = checkMarker(cid, importMap, pkg);
        } else {
          info.kind = TypeKind.OTHER;
          info.implementsMarker = checkMarker(cid, importMap, pkg);
        }
      } else if (td instanceof EnumDeclaration ed) {
        info.kind = TypeKind.ENUM;
        for (var c : ed.getEntries()) {
          info.enumConstants.add(c.getNameAsString());
        }
      }

      byQname.put(qname, info);
      byPackage.computeIfAbsentT(pkg, k -> new TList<>()).add(qname);
      cuRegistered.add(qname);

      for (var member : td.getMembers()) {
        if (member instanceof TypeDeclaration<?> nested) {
          registerType(nested, pkg, qname, importMap, cuRegistered);
        }
      }
    }

    private boolean checkMarker(
        com.github.javaparser.ast.nodeTypes.NodeWithImplements<?> nwi,
        TMap<String, String> importMap,
        String pkg) {
      for (var iface : nwi.getImplementedTypes()) {
        var qn = resolveSimpleName(iface.getNameAsString(), importMap, pkg);
        if (qn.equals(MARKER)) return true;
      }

      if (nwi instanceof ClassOrInterfaceDeclaration cid && cid.isInterface()) {
        for (var iface : cid.getExtendedTypes()) {
          var qn = resolveSimpleName(iface.getNameAsString(), importMap, pkg);
          if (qn.equals(MARKER)) return true;
        }
      }
      return false;
    }

    private boolean isImplementedBy(TypeDeclaration<?> nested, String parentSimpleName) {
      if (nested instanceof RecordDeclaration rd) {
        for (var iface : rd.getImplementedTypes()) {
          if (iface.getNameAsString().equals(parentSimpleName)) return true;
        }
      } else if (nested instanceof ClassOrInterfaceDeclaration cid) {
        for (var iface : cid.getImplementedTypes()) {
          if (iface.getNameAsString().equals(parentSimpleName)) return true;
        }
        for (var iface : cid.getExtendedTypes()) {
          if (iface.getNameAsString().equals(parentSimpleName)) return true;
        }
      }
      return false;
    }

    private String resolveSimpleName(String name, TMap<String, String> importMap, String pkg) {

      var firstDot = name.indexOf('.');
      var head = firstDot < 0 ? name : name.substring(0, firstDot);
      var tail = firstDot < 0 ? "" : name.substring(firstDot);

      var fromImport = importMap.get(head);
      if (fromImport != null) {
        return fromImport + tail;
      }

      var samePackage = byPackage.get(pkg);
      if (samePackage != null) {
        for (var qn : samePackage) {
          var lastDot = qn.lastIndexOf('.');
          var simple = lastDot < 0 ? qn : qn.substring(lastDot + 1);
          if (simple.equals(head)) return qn + tail;
        }
      }

      if (head.equals("String") || head.equals("Integer") || head.equals("Long")
          || head.equals("Double") || head.equals("Float") || head.equals("Boolean")
          || head.equals("Short") || head.equals("Byte") || head.equals("Character")
          || head.equals("Object")) {
        return "java.lang." + name;
      }
      return name + tail;
    }

    ParsedType parseType(Type t, boolean nullable) {
      if (t.isPrimitiveType()) {
        return new ParsedType(
            ParsedTypeKind.PRIMITIVE, t.asPrimitiveType().asString(), TList.of(), false);
      }
      if (t.isArrayType()) {
        var component = t.asArrayType().getComponentType();
        if (component.isPrimitiveType() && component.asPrimitiveType().asString().equals("byte")) {
          return new ParsedType(ParsedTypeKind.BYTE_ARRAY, "byte[]", TList.of(), nullable);
        }
        throw new IllegalStateException("Unsupported array type: " + t);
      }
      if (t instanceof ClassOrInterfaceType cit) {
        var name = cit.getNameAsString();

        var scopeOpt = cit.getScope();
        if (scopeOpt.isPresent()) {
          name = scopeOpt.get().toString() + "." + cit.getNameAsString();
        }
        var typeArgs = new TList<ParsedType>();
        var taOpt = cit.getTypeArguments();
        if (taOpt.isPresent()) {
          for (var ta : taOpt.get()) {
            typeArgs.add(parseType(ta, false));
          }
        }
        return new ParsedType(ParsedTypeKind.NAMED, name, typeArgs, nullable);
      }
      throw new IllegalStateException("Unsupported type: " + t);
    }

    @Nullable String resolve(String name, String contextQname) {

      if (name.contains(".") && !name.isEmpty() && Character.isLowerCase(name.charAt(0))) {

        return name;
      }
      var ctx = byQname.get(contextQname);
      if (ctx == null) return null;
      return ctx.imports.containsKey(headOf(name))
          ? ctx.imports.get(headOf(name)) + tailOf(name)
          : sameCuOrPackageResolve(name, ctx.packageName, contextQname);
    }

    private @Nullable String sameCuOrPackageResolve(
        String name, String pkg, String contextQname) {
      var head = headOf(name);
      var tail = tailOf(name);

      var current = contextQname;
      while (current.contains(".")) {
        var candidate = current + "." + name;
        if (byQname.containsKey(candidate)) return candidate;
        current = current.substring(0, current.lastIndexOf('.'));
      }

      var samePackage = byPackage.get(pkg);
      if (samePackage != null) {
        for (var qn : samePackage) {
          var last = qn.lastIndexOf('.');
          var simple = last < 0 ? qn : qn.substring(last + 1);
          if (simple.equals(head)) return qn + tail;
        }
      }

      if (TSet.of(
              "String",
              "Integer",
              "Long",
              "Double",
              "Float",
              "Boolean",
              "Short",
              "Byte",
              "Character",
              "Object")
          .contains(head)) {
        return "java.lang." + name;
      }
      return null;
    }

    private static String headOf(String n) {
      var d = n.indexOf('.');
      return d < 0 ? n : n.substring(0, d);
    }

    private static String tailOf(String n) {
      var d = n.indexOf('.');
      return d < 0 ? "" : n.substring(d);
    }
  }
}
