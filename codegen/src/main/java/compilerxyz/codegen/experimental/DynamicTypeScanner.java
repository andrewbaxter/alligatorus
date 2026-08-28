package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class DynamicTypeScanner {
    private DynamicTypeScanner() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public static TMap<ClassName, CodeBlock> scanRoot(Path runtimeSourceRoot) throws IOException {
        var out = new TMap<ClassName, CodeBlock>();
        try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
            var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
            for (var file : files) {
                var src = Files.readString(file);
                if (!src.contains("dynamicType") && !src.contains("@Builtin")) continue;
                var cu = StaticJavaParser.parse(file);
                var pkg = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");
                for (var t : cu.getTypes()) {
                    walk(pkg, t, out);
                }
            }
        }
        var mirror = new TMap<ClassName, CodeBlock>();
        for (var e : out.entrySet()) {
            var cn = e.getKey();
            if (!cn.packageName().equals("compilerxyz.handwritten")) continue;
            if (cn.simpleNames().size() < 2) continue;
            var mirrored = ClassName.get(
                    "compilerxyz.gen",
                    cn.simpleNames().get(0),
                    cn.simpleNames().subList(1, cn.simpleNames().size()).toArray(new String[0]));
            mirror.put(mirrored, e.getValue());
        }
        for (var e : mirror.entrySet()) out.put(e.getKey(), e.getValue());
        return out;
    }

    private static void walk(String pkg, TypeDeclaration<?> t, TMap<ClassName, CodeBlock> out) {
        var simple = t.getNameAsString();
        var cn = ClassName.get(pkg, simple);
        putDescriptor(out, cn, "Gen" + simple, t);
        for (var member : t.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                walkNested(pkg, simple, nested, out);
            }
        }
    }

    private static void walkNested(
            String pkg, String enclosingSimpleChain, TypeDeclaration<?> t, TMap<ClassName, CodeBlock> out) {
        var simple = t.getNameAsString();
        var cn = ClassName.get(pkg, enclosingSimpleChain, simple);
        putDescriptor(out, cn, "Gen" + enclosingSimpleChain.replace(".", "") + simple, t);
        for (var member : t.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                walkNested(pkg, enclosingSimpleChain + "." + simple, nested, out);
            }
        }
    }

    private static void putDescriptor(
            TMap<ClassName, CodeBlock> out, ClassName cn, String companionSimple, TypeDeclaration<?> t) {
        var full = hasBuiltinFullAnnotation(t);
        var handWritten = hasNullaryDynamicType(t);
        if (full && handWritten) {
            throw new IllegalStateException("@BuiltinFull type "
                    + cn.canonicalName()
                    + " also hand-declares a nullary dynamicType(); its descriptor can't be both"
                    + " generated and hand-written. Mark it @BuiltinLight (codegen generates no"
                    + " companion) or drop the hand-written dynamicType().");
        }
        if (full) {
            out.put(cn, CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.gen", companionSimple)));
        } else if (handWritten) {
            out.put(cn, CodeBlock.of("$T.dynamicType()", cn));
        }
    }

    private static boolean hasBuiltinFullAnnotation(TypeDeclaration<?> t) {
        for (var a : t.getAnnotations()) {
            var n = a.getNameAsString();
            if (n.equals("BuiltinFull") || n.equals("compilerxyz.annotations.BuiltinFull")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNullaryDynamicType(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration || t instanceof RecordDeclaration || t.isEnumDeclaration()) {
            for (var m : t.getMethods()) {
                if (isNullaryStaticDynamicType(m)) return true;
            }
        }
        return false;
    }

    private static boolean isNullaryStaticDynamicType(MethodDeclaration m) {
        if (!m.getNameAsString().equals("dynamicType")) return false;
        if (!m.isStatic()) return false;
        if (!m.getParameters().isEmpty()) return false;
        return true;
    }
}
