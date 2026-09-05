package compilerxyz.lint.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtSealable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;

public final class ConventionChecks {

    public record Finding(String check, String file, int line, String message) {}

    private final Set<String> objectMethods = Set.of("equals", "hashCode", "toString");
    private final Set<String> jdkOptional =
            Set.of("java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble");
    private static final Pattern FQN = Pattern.compile("[a-z][\\w]*(\\.[a-z][\\w]*)*(\\.[A-Z]\\w*)+");
    private static final Pattern NULLABLE_USE = Pattern.compile("@\\s*([\\w.]*\\.)?Nullable\\b");
    private static final Pattern NULLABLE_IMPORT = Pattern.compile("^\\s*import\\s+[\\w.]*\\.Nullable\\s*;");

    private final List<Finding> findings = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> generatedFiles;
    private final Map<String, Map<String, Set<String>>> fileSimpleQns = new HashMap<>();

    public ConventionChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<Finding> run(ModelIndex index) {
        for (CtType<?> t : index.types) {
            if (t instanceof CtInterface<?> iface) {
                String name = iface.getSimpleName();
                if (!name.isEmpty()) {
                    String prefix = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    for (CtMethod<?> m : iface.getMethods()) {
                        if (m.hasModifier(ModifierKind.STATIC) || m.hasModifier(ModifierKind.PRIVATE)) continue;
                        if (objectMethods.contains(m.getSimpleName())) continue;
                        if (!m.getTopDefinitions().isEmpty()) continue;
                        if (m.getSimpleName().startsWith(prefix)) continue;
                        report(
                                "InterfaceMethodNamePrefix",
                                m,
                                "Method '" + m.getSimpleName() + "' in interface '" + name
                                        + "' should be prefixed with '" + prefix + "'.");
                    }
                }
            }
        }
        for (CtBinaryOperator<?> op : index.binaryOperators) {
            if (op.getKind() == BinaryOperatorKind.INSTANCEOF && !op.isImplicit()) {
                CtTypeReference<?> operandType = op.getLeftHandOperand().getType();
                if (operandType != null) {
                    CtType<?> decl = operandType.getTypeDeclaration();
                    if (decl != null) {
                        boolean sealed = decl instanceof CtSealable s
                                && !s.getPermittedTypes().isEmpty();
                        if (sealed) {
                            report(
                                    "NoInstanceofOnSealed",
                                    op,
                                    "'" + operandType.getSimpleName()
                                            + "' is sealed; discriminate it with an exhaustive switch instead of instanceof.");
                        }
                    }
                }
            }
        }
        Set<String> scanned = new HashSet<>();
        for (CtType<?> t : index.types) {
            var pos = t.getPosition();
            if (pos == null || !pos.isValidPosition() || pos.getCompilationUnit() == null || pos.getFile() == null) {
                continue;
            }
            if (!scanned.add(pos.getFile().getPath())) continue;
            String src = pos.getCompilationUnit().getOriginalSourceCode();
            if (src == null) continue;
            String[] lines = src.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (NULLABLE_USE.matcher(lines[i]).find()
                        || NULLABLE_IMPORT.matcher(lines[i]).find()) {
                    reportAt(
                            "NoNullableType",
                            pos.getFile(),
                            i + 1,
                            1,
                            "Do not use or import @" + "Nullable; model optional values with Opt<T>.");
                }
            }
        }
        for (CtTypeReference<?> tr : index.typeReferences) {
            String qn = tr.getQualifiedName();
            if (qn == null || !qn.contains(".")) continue;
            CtElement el = tr;
            while (el != null
                    && (el.getPosition() == null
                            || !el.getPosition().isValidPosition()
                            || el.getPosition().getFile() == null)) {
                el = el.isParentInitialized() ? el.getParent() : null;
            }
            if (el == null) continue;
            fileSimpleQns
                    .computeIfAbsent(el.getPosition().getFile().getPath(), x -> new HashMap<>())
                    .computeIfAbsent(tr.getSimpleName(), x -> new HashSet<>())
                    .add(qn.replace('$', '.'));
        }
        for (CtTypeReference<?> tr : index.typeReferences) {
            if (tr.isImplicit()) continue;
            var trPos = tr.getPosition();
            String trSnippet = snippetOf(trPos);
            if (jdkOptional.contains(tr.getQualifiedName())
                    && tr.getRoleInParent() == CtRole.TYPE
                    && trSnippet.contains("Optional")) {
                report("AvoidJdkOptional", tr, "Use compilerxyz.prelude.util.Opt instead of java.util.Optional.");
            }
            if (trPos != null
                    && trPos.isValidPosition()
                    && trPos.getCompilationUnit() != null
                    && trPos.getFile() != null
                    && tr.getParent(CtImport.class) == null
                    && !trPos.getFile().getName().equals("module-info.java")) {
                String fqnQn = tr.getQualifiedName();
                if (fqnQn != null) {
                    String qnDot = fqnQn.replace('$', '.');
                    boolean written = trSnippet.equals(qnDot)
                            || trSnippet.startsWith(qnDot + "<")
                            || trSnippet.startsWith(qnDot + "[");
                    CtTypeReference<?> top = tr;
                    while (top.getDeclaringType() != null) top = top.getDeclaringType();
                    if (written
                            && qnDot.contains(".")
                            && FQN.matcher(qnDot).matches()
                            && !ambiguous(trPos.getFile().getPath(), top.getSimpleName())) {
                        report("UnnecessaryFullyQualifiedName", tr, fqnMessage(top, qnDot));
                    }
                }
            }
        }
        for (CtInvocation<?> inv : index.invocations) {
            checkFqTarget(
                    inv.getTarget(),
                    inv.getExecutable() == null ? null : inv.getExecutable().getDeclaringType());
        }
        for (CtFieldRead<?> fr : index.fieldReads) {
            checkFqTarget(
                    fr.getTarget(),
                    fr.getVariable() == null ? null : fr.getVariable().getDeclaringType());
        }
        return findings;
    }

    private void checkFqTarget(CtExpression<?> target, CtTypeReference<?> declaring) {
        if (target == null || declaring == null) return;
        var pos = target.getPosition();
        if (pos == null || !pos.isValidPosition() || pos.getCompilationUnit() == null || pos.getFile() == null) {
            return;
        }
        String snippet = snippetOf(pos);
        String qn = declaring.getQualifiedName();
        if (qn == null) return;
        String qnDot = qn.replace('$', '.');
        if (!snippet.equals(qnDot)) return;
        if (!qnDot.contains(".") || !FQN.matcher(qnDot).matches()) return;
        CtTypeReference<?> top = declaring;
        while (top.getDeclaringType() != null) top = top.getDeclaringType();
        if (ambiguous(pos.getFile().getPath(), top.getSimpleName())) return;
        report("UnnecessaryFullyQualifiedName", target, fqnMessage(top, qnDot));
    }

    private boolean ambiguous(String file, String simpleName) {
        Set<String> qns = fileSimpleQns.getOrDefault(file, Map.of()).getOrDefault(simpleName, Set.of());
        return qns.size() > 1;
    }

    private static String fqnMessage(CtTypeReference<?> top, String qnDot) {
        String importFqn = top.getQualifiedName();
        String replacement = top.getSimpleName() + qnDot.substring(importFqn.length());
        return "Import '" + importFqn + "' and use '" + replacement + "' (unless ambiguous).";
    }

    private static String snippetOf(SourcePosition pos) {
        if (pos == null || !pos.isValidPosition() || pos.getCompilationUnit() == null) return "";
        String src = pos.getCompilationUnit().getOriginalSourceCode();
        int start = pos.getSourceStart();
        int end = pos.getSourceEnd();
        if (src == null || start < 0 || end < start || end >= src.length()) return "";
        return src.substring(start, end + 1).trim();
    }

    private void report(String check, CtElement e, String message) {
        var pos = e.getPosition();
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) return;
        reportAt(check, pos.getFile(), pos.getLine(), pos.getColumn(), message);
    }

    private void reportAt(String check, File f, int line, int col, String message) {
        String abs = f.toPath().toAbsolutePath().normalize().toString();
        if (generatedFiles.contains(abs)) return;
        if (!seen.add(check + "|" + f.getPath() + "|" + line + "|" + col)) return;
        findings.add(new Finding(check, f.getPath(), line, message));
    }
}
