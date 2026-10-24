package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class CodegenDescriptorScanner {
    private CodegenDescriptorScanner() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
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
                var pkg = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");
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
            String pkg, String enclosing, TypeDeclaration<?> t, TMap<String, String> imports, TList<Entry> out) {
        var simple = t.getNameAsString();
        if (t instanceof ClassOrInterfaceDeclaration || t instanceof RecordDeclaration) {
            boolean ownerIsInterface = t instanceof ClassOrInterfaceDeclaration ci && ci.isInterface();
            var owner = enclosing.isEmpty() ? ClassName.get(pkg, simple) : ClassName.get(pkg, enclosing, simple);
            var classTypeParams = new TSet<String>();
            if (t instanceof ClassOrInterfaceDeclaration ci) {
                for (var tp : ci.getTypeParameters()) classTypeParams.add(tp.getNameAsString());
            }
            if (t instanceof RecordDeclaration rd) {
                for (var tp : rd.getTypeParameters()) classTypeParams.add(tp.getNameAsString());
                for (var component : rd.getParameters()) {
                    if (component.getAnnotations().stream()
                            .noneMatch(a -> a.getNameAsString().equals("CodegenDescriptor"))) {
                        continue;
                    }
                    out.add(new Entry(
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
                boolean isAnnotated;
                _isAnnotated:
                {
                    {
                        isAnnotated = m.getAnnotations().stream()
                                .anyMatch(a -> a.getNameAsString().equals("CodegenDescriptor"));
                        break _isAnnotated;
                    }
                }
                if (!isAnnotated) continue;
                var typeParams = new TSet<String>();
                typeParams.addAll(classTypeParams);
                for (var tp : m.getTypeParameters()) typeParams.add(tp.getNameAsString());
                var returnType = typeNameOf(m.getType(), imports, pkg, typeParams);
                var paramTypes = new TList<TypeName>();
                for (var p : m.getParameters()) {
                    paramTypes.add(typeNameOf(p.getType(), imports, pkg, typeParams));
                }
                var constantName = screamingSnake(simple) + "_" + screamingSnake(m.getNameAsString());
                out.add(new Entry(
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

    private static TypeName typeNameOf(Type t, TMap<String, String> imports, String pkg, TSet<String> typeParams) {
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
            return ArrayTypeName.of(typeNameOf(t.asArrayType().getComponentType(), imports, pkg, typeParams));
        }
        if (t.isClassOrInterfaceType()) {
            var name = scopedName(t.asClassOrInterfaceType());
            if (typeParams.contains(name)) return ClassName.get(Object.class);
            String resolve;
            _resolve:
            {
                var head = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
                var fromImportOpt = imports.getOpt(head);
                if (fromImportOpt.optIsSome()) {
                    var fromImport = fromImportOpt.optUnwrap();
                    {
                        resolve = name.contains(".") ? fromImport + name.substring(name.indexOf('.')) : fromImport;
                        break _resolve;
                    }
                }
                if (JAVA_LANG.contains(head)) {
                    resolve = "java.lang." + name;
                    break _resolve;
                }
                {
                    resolve = pkg.isEmpty() ? name : pkg + "." + name;
                    break _resolve;
                }
            }
            return ClassName.bestGuess(resolve);
        }
        throw new IllegalStateException("Unsupported @CodegenDescriptor type: " + t);
    }

    private static String scopedName(ClassOrInterfaceType t) {
        var scope = t.getScope();
        return scope.isPresent() ? scopedName(scope.get()) + "." + t.getNameAsString() : t.getNameAsString();
    }

    private static final IList<String> JAVA_LANG = IList.of(
            "String",
            "Object",
            "Integer",
            "Long",
            "Short",
            "Byte",
            "Boolean",
            "Float",
            "Double",
            "Character",
            "CharSequence",
            "Number",
            "Class");

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
