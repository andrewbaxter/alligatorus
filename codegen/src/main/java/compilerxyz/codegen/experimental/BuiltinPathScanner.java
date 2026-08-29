package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public final class BuiltinPathScanner {
    private BuiltinPathScanner() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public sealed interface Entry {
        String entryPath();
    }

    public record TypeLeaf(String entryPath, ClassName typeClass) implements Entry {}

    public record FunctionLeaf(
            String entryPath,
            long scopeId,
            String methodName,
            String ownerInternalName,
            boolean needsEvalContext,
            TList<NamedTypeRef> args,
            Opt<TypeRef> retType)
            implements Entry {}

    public static void walkType(
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
        if (t instanceof ClassOrInterfaceDeclaration || t instanceof RecordDeclaration) {
            for (var m : t.getMethods()) {
                var mpathOpt = pathFromAnnotation(m);
                if (mpathOpt.optIsNone()) continue;
                var mpath = mpathOpt.optUnwrap();
                NodeWithAnnotations<?> node = m;
                long _rScopeIdFromAnnotation;
                _scopeIdFromAnnotation:
                {
                    var opt = findBuiltinPath(node);
                    if (opt.optIsNone()) {
                        _rScopeIdFromAnnotation = 0L;
                        break _scopeIdFromAnnotation;
                    }
                    var ann = opt.optUnwrap();
                    if (ann instanceof NormalAnnotationExpr n) {
                        for (var p : n.getPairs()) {
                            if (p.getNameAsString().equals("scopeId")) {
                                var raw = p.getValue().toString();
                                if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
                                {
                                    _rScopeIdFromAnnotation = Long.parseLong(raw);
                                    break _scopeIdFromAnnotation;
                                }
                            }
                        }
                    }
                    {
                        _rScopeIdFromAnnotation = 0L;
                        break _scopeIdFromAnnotation;
                    }
                }
                long scopeId = _rScopeIdFromAnnotation;

                var args = new TList<NamedTypeRef>();
                var params = m.getParameters();
                var needsEvalContext = false;
                var firstArgIndex = 0;
                if (!params.isEmpty()
                        && params.get(0).getNameAsString().equals("vx")
                        && typeNameOf(params.get(0).getType(), imports, sourcePkg) instanceof ClassName ficn
                        && ficn.canonicalName().equals("compilerxyz.main.eval.EvalContext")) {
                    needsEvalContext = true;
                    firstArgIndex = 1;
                }
                for (int i = firstArgIndex; i < params.size(); i++) {
                    var p = params.get(i);
                    String s = p.getNameAsString();
                    String _rSnakeCase;
                    _snakeCase:
                    {
                        var sb = new StringBuilder();
                        for (int ci = 0; ci < s.length(); ci++) {
                            var c = s.charAt(ci);
                            if (Character.isUpperCase(c) && ci > 0) sb.append('_');
                            sb.append(Character.toLowerCase(c));
                        }
                        {
                            _rSnakeCase = sb.toString();
                            break _snakeCase;
                        }
                    }
                    args.add(new NamedTypeRef(_rSnakeCase, typeRefOf(p.getType(), imports, sourcePkg, scannedLeaves)));
                }
                var ownerSimple = enclosing.isEmpty() ? simple : enclosing.replace('.', '$') + "$" + simple;
                var ownerInternalName = pkg.replace('.', '/') + "/" + ownerSimple;
                Opt<TypeRef> retType = m.getType() instanceof VoidType
                        ? Opt.none()
                        : Opt.some(typeRefOf(m.getType(), imports, sourcePkg, scannedLeaves));
                out.add(new FunctionLeaf(
                        mpath, scopeId, m.getNameAsString(), ownerInternalName, needsEvalContext, args, retType));
            }
        }
        for (var member : t.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                var nextEnclosing = enclosing.isEmpty() ? simple : enclosing + "." + simple;
                walkType(pkg, sourcePkg, nextEnclosing, nested, imports, out, scannedLeaves);
            }
        }
    }

    private static Opt<AnnotationExpr> findBuiltinPath(NodeWithAnnotations<?> node) {
        for (var a : node.getAnnotations()) {
            var n = a.getNameAsString();
            if (n.equals("BuiltinPath") || n.equals("compilerxyz.prelude.annotations.BuiltinPath")) {
                return Opt.some(a);
            }
        }
        return Opt.none();
    }

    private static Opt<String> pathFromAnnotation(NodeWithAnnotations<?> node) {
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

    private static final IMap<String, ClassName> COMPOSITE_WRAPPERS = IMap.ofEntries(
            new Pair<>(
                    "compilerxyz.main.eval.EResult",
                    ClassName.get("compilerxyz.main.eval.value.eresult", "TypeEResultDynamic")),
            new Pair<>(
                    "compilerxyz.prelude.util.Opt",
                    ClassName.get("compilerxyz.main.eval.value.option", "TypeOptionDynamic")),
            new Pair<>(
                    "compilerxyz.prelude.util.TList",
                    ClassName.get("compilerxyz.main.eval.value.list", "TypeListBoxedDynamic")),
            new Pair<>(
                    "compilerxyz.prelude.util.TMap",
                    ClassName.get("compilerxyz.main.eval.value.map", "TypeMapDynamic")),
            new Pair<>(
                    "compilerxyz.prelude.util.TOrderedMap",
                    ClassName.get("compilerxyz.main.eval.value.map", "TypeMapDynamic")));

    private static final IMap<String, ClassName> VALUE_CLASS_DESCRIPTORS = IMap.ofEntries(
            new Pair<>("java.lang.String", ClassName.get("compilerxyz.main.eval.value.string", "TypeStringDynamic")),
            new Pair<>(
                    "compilerxyz.main.eval.Unique",
                    ClassName.get("compilerxyz.main.eval.value.unique", "TypeUniqueDynamic")),
            new Pair<>(
                    "compilerxyz.main.eval.expr.StagedExprCell",
                    ClassName.get("compilerxyz.main.eval.value.expr", "TypeExpr")),
            new Pair<>(
                    "compilerxyz.main.gen.AspectBind.BindResult",
                    ClassName.get("compilerxyz.main.gen", "AspectBind", "BindResult")));

    private static final ClassName TYPE_BOOL_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.bool_", "TypeBoolDynamic");

    private static final ClassName TYPE_LONG_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.long_", "TypeLongDynamic");

    private static final ClassName TYPE_NULL_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.null_", "TypeNullDynamic");

    private static TypeRef typeRefOf(
            Type t, TMap<String, String> imports, String sourcePkg, TMap<ClassName, CodeBlock> scannedLeaves) {
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
            if (wrapper != null && pt.typeArguments.size() == 1 && pt.typeArguments.get(0) instanceof ClassName inner) {
                return TypeRef.composite1(wrapper, TypeRef.leaf(inner, scannedLeaves));
            }
            if (wrapper != null
                    && pt.typeArguments.size() == 2
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
}
