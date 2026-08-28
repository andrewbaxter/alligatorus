package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import compilerxyz.util.Opt;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import java.util.Map;
import compilerxyz.util.IMap;
import compilerxyz.util.Pair;

public final class BuiltinPathScanner {
    private BuiltinPathScanner() {
    }

    static {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public sealed interface Entry {
        String entryPath();
    }

    public record TypeLeaf(String entryPath, ClassName typeClass) implements Entry {
    }

    public record FunctionLeaf(
            String entryPath,
            long scopeId,
            String methodName,
            String ownerInternalName,
            boolean needsExprContext,
            TList<NamedTypeRef> args,
            TypeRef retType)
            implements Entry {
    }

    public static TList<Entry> scanRoot(
            Path runtimeSourceRoot, TMap<ClassName, CodeBlock> scannedLeaves) throws IOException {
        var out = new TList<Entry>();
        try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
            var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
            for (var file : files) {
                var src = Files.readString(file);
                if (!src.contains("BuiltinPath")) continue;
                var cu = StaticJavaParser.parse(file);
                var pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
                var imports = buildImportMap(cu);
                for (var t : cu.getTypes()) {
                    walkType(pkg, pkg, "", t, imports, out, scannedLeaves);
                }
            }
        }
        return out;
    }

    private static void walkType(
            String pkg,
            String sourcePkg,
            String enclosing,
            TypeDeclaration<?> t,
            TMap<String, String> imports,
            TList<Entry> out,
            TMap<ClassName, CodeBlock> scannedLeaves) {
        var simple = t.getNameAsString();
        var pathOpt = pathFromAnnotation(t);
        if (pathOpt.optIsSome()) {
            ClassName cn;
            if (enclosing.isEmpty()) cn = ClassName.get(pkg, simple);
            else cn = ClassName.get(pkg, enclosing, simple);
            out.add(new TypeLeaf(pathOpt.optUnwrap(), cn));
        }
        if (t instanceof ClassOrInterfaceDeclaration
                || t instanceof RecordDeclaration) {
            for (var m : t.getMethods()) {
                var mpathOpt = pathFromAnnotation(m);
                if (mpathOpt.optIsNone()) continue;
                var mpath = mpathOpt.optUnwrap();
                var scopeId = scopeIdFromAnnotation(m);
                var args = new TList<NamedTypeRef>();
                var params = m.getParameters();
                var needsExprContext = false;
                var firstArgIndex = 0;
                if (!params.isEmpty()
                        && typeNameOf(params.get(0).getType(), imports, sourcePkg) instanceof ClassName ficn
                        && ficn.canonicalName().equals("compilerxyz.eval.ExprContext")) {
                    needsExprContext = true;
                    firstArgIndex = 1;
                }
                for (int i = firstArgIndex; i < params.size(); i++) {
                    var p = params.get(i);
                    args.add(new NamedTypeRef(snakeCase(p.getNameAsString()), typeRefOf(p.getType(), imports, sourcePkg, scannedLeaves)));
                }
                var ownerSimple = enclosing.isEmpty() ? simple : enclosing.replace('.', '$') + "$" + simple;
                var ownerInternalName = pkg.replace('.', '/') + "/" + ownerSimple;
                var retType = typeRefOf(m.getType(), imports, sourcePkg, scannedLeaves);
                out.add(
                        new FunctionLeaf(
                                mpath, scopeId, m.getNameAsString(), ownerInternalName, needsExprContext, args, retType));
            }
        }
        for (var member : t.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                var nextEnclosing = enclosing.isEmpty() ? simple : enclosing + "." + simple;
                walkType(pkg, sourcePkg, nextEnclosing, nested, imports, out, scannedLeaves);
            }
        }
    }

    private static Opt<AnnotationExpr> findBuiltinPath(
            NodeWithAnnotations<?> node) {
        for (var a : node.getAnnotations()) {
            var n = a.getNameAsString();
            if (n.equals("BuiltinPath") || n.equals("compilerxyz.annotations.BuiltinPath")) {
                return Opt.some(a);
            }
        }
        return Opt.none();
    }

    private static Opt<String> pathFromAnnotation(
            NodeWithAnnotations<?> node) {
        var opt = findBuiltinPath(node);
        if (opt.optIsNone()) return Opt.none();
        var ann = opt.optUnwrap();
        if (ann instanceof SingleMemberAnnotationExpr s) {
            var v = s.getMemberValue();
            if (v instanceof StringLiteralExpr sl) return Opt.some(sl.asString());
            throw new IllegalStateException("@BuiltinPath value must be string literal, got " + v);
        }
        if (ann instanceof NormalAnnotationExpr n) {
            for (var p : n.getPairs()) {
                if (p.getNameAsString().equals("value")) {
                    var v = p.getValue();
                    if (v instanceof StringLiteralExpr sl) return Opt.some(sl.asString());
                    throw new IllegalStateException("@BuiltinPath value must be string literal, got " + v);
                }
            }
        }
        throw new IllegalStateException("@BuiltinPath missing value");
    }

    private static long scopeIdFromAnnotation(
            NodeWithAnnotations<?> node) {
        var opt = findBuiltinPath(node);
        if (opt.optIsNone()) return 0L;
        var ann = opt.optUnwrap();
        if (ann instanceof NormalAnnotationExpr n) {
            for (var p : n.getPairs()) {
                if (p.getNameAsString().equals("scopeId")) {
                    var raw = p.getValue().toString();
                    if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
                    return Long.parseLong(raw);
                }
            }
        }
        return 0L;
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
                            ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic")),
                    new Pair<>("compilerxyz.util.TOrderedMap",
                            ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic")));

    private static final IMap<String, ClassName> VALUE_CLASS_DESCRIPTORS =
            IMap.ofEntries(
                    new Pair<>("java.lang.String",
                            ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic")),
                    new Pair<>("compilerxyz.eval.Unique",
                            ClassName.get("compilerxyz.eval.value.unique", "TypeUniqueDynamic")),
                    new Pair<>("compilerxyz.eval.expr.StagedExprCell",
                            ClassName.get("compilerxyz.eval.value.expr", "TypeExpr")),
                    new Pair<>("compilerxyz.gen.AspectBind.BindResult",
                            ClassName.get("compilerxyz.gen", "AspectBind", "BindResult")));

    private static final ClassName TYPE_BOOL_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
    private static final ClassName TYPE_INT_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.int_", "TypeIntDynamic");

    private static final ClassName TYPE_LONG_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.long_", "TypeLongDynamic");

    private static final ClassName TYPE_NULL_DYNAMIC =
            ClassName.get("compilerxyz.eval.value.null_", "TypeNullDynamic");

    private static TypeRef typeRefOf(
            Type t, TMap<String, String> imports, String sourcePkg,
            TMap<ClassName, CodeBlock> scannedLeaves) {
        if (t.isVoidType()) return TypeRef.leaf(TYPE_NULL_DYNAMIC, scannedLeaves);
        var tn = typeNameOf(t, imports, sourcePkg);
        if (tn.equals(TypeName.BOOLEAN)) return TypeRef.leaf(TYPE_BOOL_DYNAMIC, scannedLeaves);
        if (tn.equals(TypeName.LONG)) return TypeRef.leaf(TYPE_LONG_DYNAMIC, scannedLeaves);
        if (tn instanceof ClassName cn && cn.canonicalName().equals("java.lang.Object")) {
            return TypeRef.leaf(TYPE_NULL_DYNAMIC, scannedLeaves);
        }
        if (tn instanceof ClassName cn) {
            var descriptor = VALUE_CLASS_DESCRIPTORS.getOrNull(cn.canonicalName());
            if (descriptor != null) return TypeRef.leaf(descriptor, scannedLeaves);
            try {
                return TypeRef.leaf(cn, scannedLeaves);
            } catch (IllegalArgumentException e) {
            }
        }
        if (tn instanceof ParameterizedTypeName pt) {
            var wrapper = COMPOSITE_WRAPPERS.getOrNull(pt.rawType.canonicalName());
            if (wrapper != null && pt.typeArguments.size() == 1
                    && pt.typeArguments.get(0) instanceof ClassName inner) {
                return TypeRef.composite1(wrapper, TypeRef.leaf(inner, scannedLeaves));
            }
            if (wrapper != null && pt.typeArguments.size() == 2
                    && pt.typeArguments.get(0) instanceof ClassName k
                    && pt.typeArguments.get(1) instanceof ClassName v) {
                return TypeRef.composite2(wrapper, TypeRef.leaf(k, scannedLeaves), TypeRef.leaf(v, scannedLeaves));
            }
        }
        return TypeRef.opaque(tn);
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
            ClassName raw;
            if (c.getScope().isPresent()) {
                var scope = c.getScope().get();
                var scopeName = scope.getNameAsString();
                var scopeCn = classNameOf(scopeName, imports, sourcePkg);
                raw = scopeCn.nestedClass(c.getNameAsString());
            } else {
                raw = classNameOf(c.getNameAsString(), imports, sourcePkg);
            }
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
