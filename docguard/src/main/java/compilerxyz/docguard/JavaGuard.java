package compilerxyz.docguard;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class JavaGuard {
    public static final String ADDED_COMMENT = "added-comment";
    public static final String ADDED_JAVADOC = "added-javadoc";
    public static final String CHANGED_JAVADOC = "changed-javadoc";
    public static final String PARSE_ERROR = "parse-error";

    private static final Formatter FORMATTER = Formatter.createFormatter(JavaFormatterOptions.builder()
            .style(JavaFormatterOptions.Style.PALANTIR)
            .build());

    private final String file;

    public JavaGuard(String file) {
        this.file = file;
    }

    public FixResult stripComments(String current) {
        Optional<CompilationUnit> parsed = parse(current);
        if (parsed.isEmpty()) {
            return new FixResult(current, false, List.of());
        }
        CompilationUnit cu = parsed.get();
        LexicalPreservingPrinter.setup(cu);
        boolean touched = false;
        for (Comment c : nonJavadocComments(cu)) {
            c.remove();
            touched = true;
        }
        if (!touched) {
            return new FixResult(current, false, List.of());
        }
        return new FixResult(LexicalPreservingPrinter.print(cu), true, List.of());
    }

    private static String format(String source) {
        try {
            return FORMATTER.formatSource(source);
        } catch (FormatterException e) {
            return source;
        }
    }

    public List<Violation> check(Optional<String> base, String current, boolean strict) {
        Optional<CompilationUnit> parsed = parse(current);
        if (parsed.isEmpty()) {
            return List.of(new Violation(file, 0, PARSE_ERROR, "docguard could not parse this file; cannot verify"));
        }
        CompilationUnit cu = parsed.get();
        List<Violation> out = new ArrayList<>();
        for (Comment c : nonJavadocComments(cu)) {
            out.add(new Violation(file, lineOf(c), ADDED_COMMENT, "non-Javadoc comment: " + preview(c)));
        }
        if (strict) {
            Map<String, String> baseDoc =
                    base.flatMap(this::parse).map(JavaGuard::javadocByKey).orElse(Map.of());
            for (Node node : declarations(cu)) {
                String key = keyOf(node);
                if (key == null) {
                    continue;
                }
                String cur =
                        javadocContent(node).map(c -> normalize(c.getContent())).orElse(null);
                String was = baseDoc.get(key);
                if (was == null && cur != null) {
                    out.add(new Violation(file, lineOf(node), ADDED_JAVADOC, "Javadoc added on " + key));
                } else if (was != null && !was.equals(cur)) {
                    out.add(new Violation(file, lineOf(node), CHANGED_JAVADOC, "Javadoc changed on " + key));
                }
            }
        }
        return out;
    }

    public FixResult fix(Optional<String> base, String current, boolean strict) {
        Optional<CompilationUnit> parsed = parse(current);
        if (parsed.isEmpty()) {
            return new FixResult(
                    current, false, List.of(new Violation(file, 0, PARSE_ERROR, "could not parse; left unchanged")));
        }
        CompilationUnit cu = parsed.get();
        LexicalPreservingPrinter.setup(cu);
        boolean touched = false;
        for (Comment c : nonJavadocComments(cu)) {
            c.remove();
            touched = true;
        }
        if (strict) {
            Map<String, JavadocComment> baseDoc =
                    base.flatMap(this::parse).map(JavaGuard::javadocNodeByKey).orElse(Map.of());
            for (Node node : declarations(cu)) {
                String key = keyOf(node);
                if (key == null) {
                    continue;
                }
                String cur =
                        javadocContent(node).map(c -> normalize(c.getContent())).orElse(null);
                JavadocComment was = baseDoc.get(key);
                String wasNorm = was == null ? null : normalize(was.getContent());
                if (was == null && cur != null) {
                    node.getComment().filter(Comment::isJavadocComment).ifPresent(Comment::remove);
                    touched = true;
                } else if (was != null && !equalDoc(wasNorm, cur)) {
                    node.setComment(new JavadocComment(was.getContent()));
                    touched = true;
                }
            }
        }
        String printed = touched ? LexicalPreservingPrinter.print(cu) : current;
        String output = format(printed);
        Optional<CompilationUnit> recheck = parse(output);
        if (recheck.isEmpty()) {
            return new FixResult(
                    current,
                    false,
                    List.of(new Violation(file, 0, PARSE_ERROR, "fix produced unparseable output; left unchanged")));
        }
        List<Violation> residual = check(base, output, strict);
        return new FixResult(output, !output.equals(current), residual);
    }

    private static boolean equalDoc(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static final ParserConfiguration CONFIG =
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

    private Optional<CompilationUnit> parse(String source) {
        ParseResult<CompilationUnit> r = new JavaParser(CONFIG).parse(source);
        return r.isSuccessful() ? r.getResult() : Optional.empty();
    }

    private static List<Comment> nonJavadocComments(CompilationUnit cu) {
        List<Comment> out = new ArrayList<>();
        for (Comment c : cu.getAllContainedComments()) {
            if (!c.isJavadocComment()) {
                out.add(c);
            }
        }
        cu.getComment().filter(c -> !c.isJavadocComment()).ifPresent(out::add);
        return out;
    }

    private static Optional<Comment> javadocContent(Node node) {
        return node.getComment().filter(Comment::isJavadocComment);
    }

    private static List<Node> declarations(CompilationUnit cu) {
        List<Node> out = new ArrayList<>();
        out.addAll(cu.findAll(TypeDeclaration.class));
        out.addAll(cu.findAll(MethodDeclaration.class));
        out.addAll(cu.findAll(ConstructorDeclaration.class));
        out.addAll(cu.findAll(CompactConstructorDeclaration.class));
        out.addAll(cu.findAll(FieldDeclaration.class));
        out.addAll(cu.findAll(EnumConstantDeclaration.class));
        out.addAll(cu.findAll(AnnotationMemberDeclaration.class));
        return out;
    }

    private static Map<String, String> javadocByKey(CompilationUnit cu) {
        Map<String, String> out = new HashMap<>();
        for (Node node : declarations(cu)) {
            String key = keyOf(node);
            if (key == null) {
                continue;
            }
            javadocContent(node).ifPresent(c -> out.put(key, normalize(c.getContent())));
        }
        return out;
    }

    private static Map<String, JavadocComment> javadocNodeByKey(CompilationUnit cu) {
        Map<String, JavadocComment> out = new HashMap<>();
        for (Node node : declarations(cu)) {
            String key = keyOf(node);
            if (key == null) {
                continue;
            }
            node.getComment()
                    .filter(Comment::isJavadocComment)
                    .map(c -> (JavadocComment) c)
                    .ifPresent(c -> out.put(key, c));
        }
        return out;
    }

    private static String keyOf(Node node) {
        if (node instanceof TypeDeclaration<?> t) {
            return "type:" + fqn(t);
        }
        if (node instanceof MethodDeclaration m) {
            return enclosing(node) + "#m:" + m.getSignature().asString();
        }
        if (node instanceof ConstructorDeclaration c) {
            return enclosing(node) + "#c:" + c.getSignature().asString();
        }
        if (node instanceof CompactConstructorDeclaration cc) {
            return enclosing(node) + "#cc:" + cc.getNameAsString();
        }
        if (node instanceof FieldDeclaration f) {
            String names = f.getVariables().stream()
                    .map(VariableDeclarator::getNameAsString)
                    .collect(Collectors.joining(","));
            return enclosing(node) + "#f:" + names;
        }
        if (node instanceof EnumConstantDeclaration e) {
            return enclosing(node) + "#e:" + e.getNameAsString();
        }
        if (node instanceof AnnotationMemberDeclaration a) {
            return enclosing(node) + "#am:" + a.getNameAsString();
        }
        return null;
    }

    private static String enclosing(Node node) {
        return node.findAncestor(TypeDeclaration.class).map(JavaGuard::fqn).orElse("");
    }

    private static String fqn(TypeDeclaration<?> t) {
        Deque<String> parts = new ArrayDeque<>();
        Node cur = t;
        while (cur != null) {
            if (cur instanceof TypeDeclaration<?> td) {
                parts.addFirst(td.getNameAsString());
            }
            cur = cur.getParentNode().orElse(null);
        }
        String pkg = t.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(p -> p.getNameAsString())
                .orElse("");
        String simple = String.join(".", parts);
        return pkg.isEmpty() ? simple : pkg + "." + simple;
    }

    private static String normalize(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\n", -1)) {
            String s = line.strip();
            if (s.startsWith("*")) {
                s = s.substring(1).strip();
            }
            if (!s.isEmpty()) {
                sb.append(s).append('\n');
            }
        }
        return sb.toString();
    }

    private static int lineOf(Node node) {
        return node.getBegin().map(p -> p.line).orElse(0);
    }

    private static String preview(Comment c) {
        String s = c.getContent().strip().replace('\n', ' ');
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    public record FixResult(String output, boolean changed, List<Violation> residual) {}
}
