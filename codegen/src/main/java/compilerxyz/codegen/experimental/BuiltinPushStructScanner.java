package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.PushStructsGen;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.PushMethod;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import compilerxyz.util.IMap;
import compilerxyz.util.Pair;

public final class BuiltinPushStructScanner {
  private BuiltinPushStructScanner() {}

  static {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName EXPR_CONTEXT =
      ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName TYPE_STRING_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_MAP_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic");
  private static final ClassName TYPE_CONSTKEY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.constkey", "TypeConstkeyDynamic");
  private static final ClassName TYPE_EXPR =
      ClassName.get("compilerxyz.eval.value.expr", "TypeExpr");

  private static final IMap<String, ClassName> VALUE_CLASS_DESCRIPTORS =
      IMap.ofEntries(
          new Pair<>("java.lang.String", TYPE_STRING_DYNAMIC),
          new Pair<>("java.lang.Boolean", TYPE_BOOL_DYNAMIC),
          new Pair<>("compilerxyz.eval.ConstKey", TYPE_CONSTKEY_DYNAMIC),
          new Pair<>("compilerxyz.eval.expr.StagedExprCell", TYPE_EXPR));

  public static TList<PushStructsGen.PushStructSpec> scanRoot(
      Path runtimeSourceRoot, TMap<ClassName, CodeBlock> scannedLeaves)
      throws IOException {
    var out = new TList<PushStructsGen.PushStructSpec>();
    try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
      var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
      for (var file : files) {
        var src = Files.readString(file);
        if (!src.contains("BuiltinPushStruct")) continue;
        var cu = StaticJavaParser.parse(file);
        var imports = buildImportMap(cu);
        var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
        for (var t : cu.getTypes()) {
          if (!(t instanceof ClassOrInterfaceDeclaration c) || c.isInterface()) continue;
          AnnotationExpr ann = null;
          for (var a : c.getAnnotations()) {
            var n = a.getNameAsString();
            if (n.equals("BuiltinPushStruct")
                || n.equals("compilerxyz.annotations.BuiltinPushStruct")) {
              ann = a;
              break;
            }
          }
          if (ann == null) continue;
          out.add(extractStruct(c, ann, imports, pkg, scannedLeaves));
        }
      }
    }
    return out;
  }

  private static PushStructsGen.PushStructSpec extractStruct(
      ClassOrInterfaceDeclaration cls,
      AnnotationExpr ann,
      TMap<String, String> imports,
      String pkg,
      TMap<ClassName, CodeBlock> scannedLeaves) {
    var name = cls.getNameAsString();

    var dataFields = new TList<NamedTypeRef>();
    var ctors = cls.getConstructors();
    if (!ctors.isEmpty()) {
      var ctor = ctors.get(0);
      for (var p : ctor.getParameters()) {
        var srcName = snakeStripTrailing(p.getNameAsString());
        dataFields.add(new NamedTypeRef(srcName, typeRefOf(p.getType(), imports, pkg, scannedLeaves)));
      }
    }

    var fields = new TList<PushStructsGen.PushField>();
    for (var f : cls.getFields()) {
      if (f.isStatic() || !f.hasModifier(Modifier.Keyword.PUBLIC) || !f.isFinal()) continue;
      for (var v : f.getVariables()) {
        var javaName = v.getNameAsString();
        var srcName = snakeStripTrailing(javaName);
        var tr = typeRefOf(f.getElementType(), imports, pkg, scannedLeaves);
        fields.add(
            new PushStructsGen.PushField(srcName, javaName, tr, CodeBlock.of("")));
      }
    }

    var methods = new TList<PushMethod>();
    for (var m : cls.getMethods()) {
      if (m.isStatic() || !m.hasModifier(Modifier.Keyword.PUBLIC)) continue;
      var params = m.getParameters();
      if (params.isEmpty()) continue;
      var first = params.get(0);
      if (!isExprContext(first.getType(), imports, pkg)) continue;
      var srcMethodName = snakeStripTrailing(m.getNameAsString());
      var args = new TList<NamedTypeRef>();
      for (int i = 1; i < params.size(); i++) {
        var p = params.get(i);
        var argSrcName = snakeStripTrailing(p.getNameAsString());
        args.add(new NamedTypeRef(argSrcName, typeRefOf(p.getType(), imports, pkg, scannedLeaves)));
      }
      var retExpr = returnExpr(m.getType(), imports, pkg, scannedLeaves);
      var retClass = returnTypeRef(m.getType(), imports, pkg, scannedLeaves);
      methods.add(new PushMethod(srcMethodName, args, retExpr, CodeBlock.of(""), "", retClass));
    }

    return new PushStructsGen.PushStructSpec(name, dataFields, fields, methods);
  }

  private static boolean isExprContext(Type t, TMap<String, String> imports, String pkg) {
    if (!(t instanceof ClassOrInterfaceType c)) return false;
    var simple = c.getNameAsString();
    var fqn = resolve(simple, imports, pkg);
    return fqn.equals("compilerxyz.eval.ExprContext");
  }

  private static String returnExpr(
      Type t, TMap<String, String> imports, String pkg,
      TMap<ClassName, CodeBlock> scannedLeaves) {
    if (t instanceof VoidType) return "()";
    var tr = typeRefOf(t, imports, pkg, scannedLeaves);
    return tr.dynamicType().toString();
  }

  private static TypeRef returnTypeRef(
      Type t, TMap<String, String> imports, String pkg,
      TMap<ClassName, CodeBlock> scannedLeaves) {
    if (t instanceof VoidType) return TypeRef.opaque(TypeName.VOID);
    return typeRefOf(t, imports, pkg, scannedLeaves);
  }

  private static long literalLong(MemberValuePair p) {
    var raw = p.getValue().toString();
    if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
    return Long.parseLong(raw);
  }

  private static String snakeStripTrailing(String name) {
    var sb = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      var c = name.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toLowerCase(c));
    }
    var s = sb.toString();
    if (s.endsWith("_") && s.length() > 1) s = s.substring(0, s.length() - 1);
    return s;
  }

  private static TypeRef typeRefOf(
      Type t, TMap<String, String> imports, String pkg,
      TMap<ClassName, CodeBlock> scannedLeaves) {
    if (t instanceof ClassOrInterfaceType cit
        && cit.getNameAsString().equals("Opt")
        && cit.getTypeArguments().isPresent()
        && cit.getTypeArguments().get().size() == 1) {
      var innerRef = typeRefOf(cit.getTypeArguments().get().get(0), imports, pkg, scannedLeaves);
      return TypeRef.composite1(
          ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic"), innerRef);
    }
    if (t instanceof ClassOrInterfaceType cit
        && cit.getNameAsString().equals("EResult")
        && cit.getTypeArguments().isPresent()
        && cit.getTypeArguments().get().size() == 1) {
      var innerRef = typeRefOf(cit.getTypeArguments().get().get(0), imports, pkg, scannedLeaves);
      return TypeRef.composite1(
          ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic"), innerRef);
    }
    var tn = typeNameOf(t, imports, pkg);
    if (tn.equals(TypeName.BOOLEAN)) return TypeRef.leaf(TYPE_BOOL_DYNAMIC, scannedLeaves);
    if (tn instanceof ClassName cn) {
      var descriptor = VALUE_CLASS_DESCRIPTORS.getOrNull(cn.canonicalName());
      if (descriptor != null) return TypeRef.leaf(descriptor, scannedLeaves);
      try {
        return TypeRef.leaf(cn, scannedLeaves);
      } catch (IllegalArgumentException e) {
        // fall through
      }
    }
    if (tn instanceof ParameterizedTypeName pt
        && pt.rawType.canonicalName().equals("compilerxyz.util.TOrderedMap")
        && pt.typeArguments.size() == 2
        && pt.typeArguments.get(0) instanceof ClassName k
        && pt.typeArguments.get(1) instanceof ClassName v) {
      var kDesc = VALUE_CLASS_DESCRIPTORS.getOrNull(k.canonicalName());
      var vDesc = VALUE_CLASS_DESCRIPTORS.getOrNull(v.canonicalName());
      return TypeRef.composite2(
          TYPE_MAP_DYNAMIC,
          TypeRef.leaf(kDesc != null ? kDesc : k, scannedLeaves),
          TypeRef.leaf(vDesc != null ? vDesc : v, scannedLeaves));
    }
    return TypeRef.opaque(tn);
  }

  private static TypeName typeNameOf(Type t, TMap<String, String> imports, String pkg) {
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
    if (t instanceof VoidType) return TypeName.VOID;
    if (t instanceof ClassOrInterfaceType c) {
      var raw = classNameOf(c.getNameAsString(), imports, pkg);
      if (c.getTypeArguments().isPresent() && !c.getTypeArguments().get().isEmpty()) {
        var argsList = c.getTypeArguments().get();
        var argNames = new TypeName[argsList.size()];
        for (int i = 0; i < argsList.size(); i++) {
          argNames[i] = typeNameOf(argsList.get(i), imports, pkg);
        }
        return ParameterizedTypeName.get(raw, argNames);
      }
      return raw;
    }
    throw new IllegalStateException("Unhandled type: " + t + " (" + t.getClass() + ")");
  }

  private static ClassName classNameOf(String simple, TMap<String, String> imports, String pkg) {
    var fqn = resolve(simple, imports, pkg);
    var idx = fqn.lastIndexOf('.');
    return ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
  }

  private static String resolve(String simple, TMap<String, String> imports, String pkg) {
    var fqn = imports.getOrNull(simple);
    if (fqn != null) return fqn;
    try {
      return Class.forName("java.lang." + simple).getName();
    } catch (ClassNotFoundException e) {
      return pkg + "." + simple;
    }
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
}
