package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import compilerxyz.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class BuiltinIdentityScanner {
    private BuiltinIdentityScanner() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public record IdentitySpec(String name, long value, boolean standalone) {}

    public static TList<IdentitySpec> scanRoot(Path runtimeSourceRoot) throws IOException {
        var out = new TList<IdentitySpec>();
        try (Stream<Path> stream = Files.walk(runtimeSourceRoot)) {
            var files = stream.filter(p -> p.toString().endsWith(".java")).toList();
            for (var file : files) {
                var src = Files.readString(file);
                if (!src.contains("@BuiltinFull(") && !src.contains("@BuiltinLight(")) continue;
                var cu = StaticJavaParser.parse(file);
                for (var t : cu.getTypes()) {
                    collect(t, "", out);
                }
            }
        }
        return out;
    }

    private static void collect(TypeDeclaration<?> t, String enclosingChain, TList<IdentitySpec> out) {
        var mySimple = t.getNameAsString();
        for (var a : t.getAnnotations()) {
            var n = a.getNameAsString();
            var isBuiltin = n.equals("BuiltinFull") || n.equals("compilerxyz.annotations.BuiltinFull");
            var isIdentityOnly = n.equals("BuiltinLight") || n.equals("compilerxyz.annotations.BuiltinLight");
            if (isBuiltin || isIdentityOnly) {
                Long uniqueId = null;
                if (a instanceof NormalAnnotationExpr n2) {
                    for (var p : n2.getPairs()) {
                        if (p.getNameAsString().equals("uniqueId"))
                            uniqueId = parseLong(p.getValue().toString());
                    }
                } else if (a instanceof SingleMemberAnnotationExpr s) {
                    uniqueId = parseLong(s.getMemberValue().toString());
                }
                if (uniqueId == null) {
                    throw new IllegalStateException("@BuiltinFull/@BuiltinLight missing uniqueId on " + mySimple);
                }
                var useChained = !enclosingChain.isEmpty();
                var name = useChained ? enclosingChain + mySimple : mySimple;
                out.add(new IdentitySpec(snakeCase(name), uniqueId, isIdentityOnly));
            }
        }
        var nextChain = enclosingChain.isEmpty() ? mySimple : enclosingChain + mySimple;
        for (var member : t.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                collect(nested, nextChain, out);
            }
        }
    }

    private static long parseLong(String raw) {
        if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
        return Long.parseLong(raw);
    }

    private static String snakeCase(String className) {
        var sb = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
