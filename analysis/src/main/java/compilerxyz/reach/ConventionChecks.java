package compilerxyz.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import spoon.reflect.CtModel;
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
import spoon.reflect.visitor.filter.TypeFilter;

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

    public List<Finding> run(CtModel model) {
        for (CtType<?> t : model.getElements(new TypeFilter<>(CtType.class))) {
            if (t instanceof CtInterface<?> iface) {
                String _cimName = iface.getSimpleName();
                if (!_cimName.isEmpty()) {
                    String _cimPrefix = Character.toLowerCase(_cimName.charAt(0)) + _cimName.substring(1);
                    for (CtMethod<?> _cimM : iface.getMethods()) {
                        if (_cimM.hasModifier(ModifierKind.STATIC) || _cimM.hasModifier(ModifierKind.PRIVATE)) continue;
                        if (objectMethods.contains(_cimM.getSimpleName())) continue;
                        if (!_cimM.getTopDefinitions().isEmpty()) continue;
                        if (_cimM.getSimpleName().startsWith(_cimPrefix)) continue;
                        report(
                                "InterfaceMethodNamePrefix",
                                _cimM,
                                "Method '" + _cimM.getSimpleName() + "' in interface '" + _cimName
                                        + "' should be prefixed with '" + _cimPrefix + "'.");
                    }
                }
            }
        }
        for (CtBinaryOperator<?> op : model.getElements(new TypeFilter<>(CtBinaryOperator.class))) {
            if (op.getKind() == BinaryOperatorKind.INSTANCEOF && !op.isImplicit()) {
                CtTypeReference<?> _cisOperandType = op.getLeftHandOperand().getType();
                if (_cisOperandType != null) {
                    CtType<?> _cisDecl = _cisOperandType.getTypeDeclaration();
                    if (_cisDecl != null) {
                        boolean _cisSealed = _cisDecl instanceof CtSealable _s
                                && !_s.getPermittedTypes().isEmpty();
                        if (_cisSealed) {
                            report(
                                    "NoInstanceofOnSealed",
                                    op,
                                    "'" + _cisOperandType.getSimpleName()
                                            + "' is sealed; discriminate it with an exhaustive switch instead of instanceof.");
                        }
                    }
                }
            }
        }
        Set<String> _nulScanned = new HashSet<>();
        for (CtType<?> t : model.getElements(new TypeFilter<>(CtType.class))) {
            var _nulPos = t.getPosition();
            if (_nulPos == null
                    || !_nulPos.isValidPosition()
                    || _nulPos.getCompilationUnit() == null
                    || _nulPos.getFile() == null) {
                continue;
            }
            if (!_nulScanned.add(_nulPos.getFile().getPath())) continue;
            String _nulSrc = _nulPos.getCompilationUnit().getOriginalSourceCode();
            if (_nulSrc == null) continue;
            String[] _nulLines = _nulSrc.split("\n", -1);
            for (int i = 0; i < _nulLines.length; i++) {
                if (NULLABLE_USE.matcher(_nulLines[i]).find()
                        || NULLABLE_IMPORT.matcher(_nulLines[i]).find()) {
                    reportAt(
                            "NoNullableType",
                            _nulPos.getFile(),
                            i + 1,
                            1,
                            "Do not use or import @" + "Nullable; model optional values with Opt<T>.");
                }
            }
        }
        for (CtTypeReference<?> tr : model.getElements(new TypeFilter<>(CtTypeReference.class))) {
            String _ambQn = tr.getQualifiedName();
            if (_ambQn == null || !_ambQn.contains(".")) continue;
            CtElement _ambEl = tr;
            while (_ambEl != null
                    && (_ambEl.getPosition() == null
                            || !_ambEl.getPosition().isValidPosition()
                            || _ambEl.getPosition().getFile() == null)) {
                _ambEl = _ambEl.isParentInitialized() ? _ambEl.getParent() : null;
            }
            if (_ambEl == null) continue;
            fileSimpleQns
                    .computeIfAbsent(_ambEl.getPosition().getFile().getPath(), x -> new HashMap<>())
                    .computeIfAbsent(tr.getSimpleName(), x -> new HashSet<>())
                    .add(_ambQn.replace('$', '.'));
        }
        for (CtTypeReference<?> tr : model.getElements(new TypeFilter<>(CtTypeReference.class))) {
            if (tr.isImplicit()) continue;
            var _ssTrPos = tr.getPosition();
            String _ssTrSnippet = snippetOf(_ssTrPos);
            if (jdkOptional.contains(tr.getQualifiedName())
                    && tr.getRoleInParent() == CtRole.TYPE
                    && _ssTrSnippet.contains("Optional")) {
                report("AvoidJdkOptional", tr, "Use compilerxyz.util.Opt instead of java.util.Optional.");
            }
            if (_ssTrPos != null
                    && _ssTrPos.isValidPosition()
                    && _ssTrPos.getCompilationUnit() != null
                    && _ssTrPos.getFile() != null
                    && tr.getParent(CtImport.class) == null
                    && !_ssTrPos.getFile().getName().equals("module-info.java")) {
                String _fqnQn = tr.getQualifiedName();
                if (_fqnQn != null) {
                    String _fqnQnDot = _fqnQn.replace('$', '.');
                    boolean _fqnWritten = _ssTrSnippet.equals(_fqnQnDot)
                            || _ssTrSnippet.startsWith(_fqnQnDot + "<")
                            || _ssTrSnippet.startsWith(_fqnQnDot + "[");
                    if (_fqnWritten
                            && _fqnQnDot.contains(".")
                            && FQN.matcher(_fqnQnDot).matches()
                            && !ambiguous(_ssTrPos.getFile().getPath(), tr.getSimpleName())) {
                        CtTypeReference<?> _fqnTop = tr;
                        while (_fqnTop.getDeclaringType() != null) _fqnTop = _fqnTop.getDeclaringType();
                        report("UnnecessaryFullyQualifiedName", tr, fqnMessage(_fqnTop, _fqnQnDot));
                    }
                }
            }
        }
        for (CtInvocation<?> inv : model.getElements(new TypeFilter<>(CtInvocation.class))) {
            checkFqTarget(
                    inv.getTarget(),
                    inv.getExecutable() == null ? null : inv.getExecutable().getDeclaringType());
        }
        for (CtFieldRead<?> fr : model.getElements(new TypeFilter<>(CtFieldRead.class))) {
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
        if (ambiguous(pos.getFile().getPath(), declaring.getSimpleName())) return;
        CtTypeReference<?> top = declaring;
        while (top.getDeclaringType() != null) top = top.getDeclaringType();
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
