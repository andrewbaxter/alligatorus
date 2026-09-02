package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TMap;

public final class BuiltinPushStructScanner {
    private BuiltinPushStructScanner() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    private static final ClassName TYPE_STRING_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.string", "TypeStringDynamic");
    private static final ClassName TYPE_BOOL_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.bool_", "TypeBoolDynamic");
    private static final ClassName TYPE_MAP_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.map", "TypeMapDynamic");
    private static final ClassName TYPE_CONSTKEY_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.constkey", "TypeConstkeyDynamic");
    private static final ClassName TYPE_EXPR = ClassName.get("compilerxyz.main.eval.value.expr", "TypeExpr");

    private static final IMap<String, ClassName> VALUE_CLASS_DESCRIPTORS = IMap.ofEntries(
            new Pair<>("java.lang.String", TYPE_STRING_DYNAMIC),
            new Pair<>(
                    "compilerxyz.main.eval.Code", ClassName.get("compilerxyz.main.eval.value.code", "TypeCodeDynamic")),
            new Pair<>("java.lang.Boolean", TYPE_BOOL_DYNAMIC),
            new Pair<>("compilerxyz.main.eval.ConstKey", TYPE_CONSTKEY_DYNAMIC),
            new Pair<>("compilerxyz.main.eval.expr.StagedExprCell", TYPE_EXPR));

    public static String snakeStripTrailing(String name) {
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

    public static TypeRef typeRefOf(
            Type t, TMap<String, String> imports, String pkg, TMap<ClassName, CodeBlock> scannedLeaves) {
        if (t instanceof ClassOrInterfaceType cit
                && cit.getNameAsString().equals("Opt")
                && cit.getTypeArguments().isPresent()
                && cit.getTypeArguments().get().size() == 1) {
            var innerRef = typeRefOf(cit.getTypeArguments().get().get(0), imports, pkg, scannedLeaves);
            return TypeRef.composite1(
                    ClassName.get("compilerxyz.main.eval.value.option", "TypeOptionDynamic"), innerRef);
        }
        if (t instanceof ClassOrInterfaceType cit
                && cit.getNameAsString().equals("EResult")
                && cit.getTypeArguments().isPresent()
                && cit.getTypeArguments().get().size() == 1) {
            var innerRef = typeRefOf(cit.getTypeArguments().get().get(0), imports, pkg, scannedLeaves);
            return TypeRef.composite1(
                    ClassName.get("compilerxyz.main.eval.value.eresult", "TypeEResultDynamic"), innerRef);
        }
        var tn = typeNameOf(t, imports, pkg);
        if (tn.equals(TypeName.BOOLEAN)) return TypeRef.leaf(TYPE_BOOL_DYNAMIC, scannedLeaves);
        if (tn instanceof ClassName cn) {
            var descriptor = VALUE_CLASS_DESCRIPTORS.getOrNull(cn.canonicalName());
            if (descriptor != null) return TypeRef.leaf(descriptor, scannedLeaves);
            try {
                return TypeRef.leaf(cn, scannedLeaves);
            } catch (IllegalArgumentException e) {
            }
        }
        if (tn instanceof ParameterizedTypeName pt
                && pt.rawType.canonicalName().equals("compilerxyz.prelude.util.TOrderedMap")
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
            String simple = c.getNameAsString();
            ClassName _rClassNameOf;
            _classNameOf:
            {
                var fqn = resolve(simple, imports, pkg);
                var idx = fqn.lastIndexOf('.');
                {
                    _rClassNameOf = ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
                    break _classNameOf;
                }
            }
            ClassName raw = _rClassNameOf;

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

    public static String resolve(String simple, TMap<String, String> imports, String pkg) {
        var fqn = imports.getOrNull(simple);
        if (fqn != null) return fqn;
        try {
            return Class.forName("java.lang." + simple).getName();
        } catch (ClassNotFoundException e) {
            return pkg + "." + simple;
        }
    }
}
