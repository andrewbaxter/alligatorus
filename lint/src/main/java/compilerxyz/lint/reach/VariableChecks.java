package compilerxyz.lint.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;

public final class VariableChecks {

    private final List<ConventionChecks.Finding> findings = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> generatedFiles;
    private final Map<String, Set<String>> accessNamesByScope = new HashMap<>();

    public VariableChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<ConventionChecks.Finding> run(ModelIndex index) {
        for (CtLocalVariable<?> local : index.localVariables) {
            if (local.isImplicit()) continue;
            var parent = local.isParentInitialized() ? local.getParent() : null;
            var isPattern = false;
            while (parent != null) {
                if (parent.getClass().getSimpleName().contains("Pattern")) {
                    isPattern = true;
                    break;
                }
                if (parent instanceof CtMethod
                        || parent instanceof CtConstructor
                        || parent instanceof CtLambda
                        || !parent.isParentInitialized()) {
                    break;
                }
                parent = parent.getParent();
            }
            if (isPattern || local.getParent() instanceof CtTryWithResource) continue;
            if (usedInEnclosingBody(index, local, local.getSimpleName())) continue;
            report(
                    "VariableUnused",
                    local,
                    "local variable '" + local.getSimpleName()
                            + "' is never read (write-only counts as unused); remove it (keep the initializer as a"
                            + " bare statement if it has side effects).");
        }

        var contractDecl = new LinkedHashMap<String, CtMethod<?>>();
        var contractParamUsed = new HashMap<String, boolean[]>();

        for (CtMethod<?> m : index.methods) {
            if (m.getBody() == null) continue;
            var tops = new ArrayList<CtMethod<?>>();
            try {
                for (var top : m.getTopDefinitions()) {
                    if (top.getDeclaringType() instanceof CtInterface<?>) tops.add(top);
                }
            } catch (RuntimeException e) {
                tops.clear();
            }
            if (tops.isEmpty()) {
                var override = false;
                for (CtAnnotation<?> a : m.getAnnotations()) {
                    if ("Override".equals(a.getAnnotationType().getSimpleName())) {
                        override = true;
                        break;
                    }
                }
                if (override) continue;
                for (CtParameter<?> p : m.getParameters()) {
                    if (p.isImplicit() || usedInEnclosingBody(index, m, p.getSimpleName())) continue;
                    report(
                            "ParameterUnused",
                            p,
                            "parameter '" + p.getSimpleName() + "' of '" + ownerName(m) + "." + m.getSimpleName()
                                    + "' is never read; remove it.");
                }
            } else {
                recordContractUsage(index, tops, m, m.getParameters(), contractDecl, contractParamUsed);
            }
        }

        for (CtConstructor<?> c : index.constructors) {
            if (c.isImplicit() || c.getBody() == null) continue;
            for (CtParameter<?> p : c.getParameters()) {
                if (p.isImplicit() || usedInEnclosingBody(index, c, p.getSimpleName())) continue;
                report(
                        "ParameterUnused",
                        p,
                        "constructor parameter '" + p.getSimpleName() + "' of '"
                                + c.getDeclaringType().getQualifiedName() + "' is never read; remove it.");
            }
        }

        for (CtLambda<?> l : index.lambdas) {
            CtMethod<?> overridden;
            try {
                overridden = l.getOverriddenMethod();
            } catch (RuntimeException e) {
                continue;
            }
            if (overridden == null || !(overridden.getDeclaringType() instanceof CtInterface<?>)) continue;
            recordContractUsage(index, List.of(overridden), l, l.getParameters(), contractDecl, contractParamUsed);
        }

        for (Map.Entry<String, CtMethod<?>> entry : contractDecl.entrySet()) {
            var contract = entry.getValue();
            var params = contract.getParameters();
            var usedFlags = contractParamUsed.get(entry.getKey());
            for (int i = 0; i < params.size() && i < usedFlags.length; i++) {
                if (usedFlags[i]) continue;
                report(
                        "InterfaceParameterUnused",
                        params.get(i),
                        "parameter '" + params.get(i).getSimpleName() + "' of '" + ownerName(contract) + "."
                                + contract.getSimpleName() + "' is unused in every implementation; remove it from the"
                                + " contract.");
            }
        }
        return findings;
    }

    private void recordContractUsage(
            ModelIndex index,
            List<CtMethod<?>> tops,
            CtElement impl,
            List<? extends CtParameter<?>> implParams,
            Map<String, CtMethod<?>> contractDecl,
            Map<String, boolean[]> contractParamUsed) {
        for (var top : tops) {
            var owner = top.getDeclaringType();
            if (owner == null) continue;
            var bridged = false;
            for (CtAnnotation<?> a : owner.getAnnotations()) {
                if (a.getAnnotationType().getSimpleName().startsWith("Builtin")) {
                    bridged = true;
                    break;
                }
            }
            if (bridged) continue;
            var key = owner.getQualifiedName() + "#" + top.getSignature();
            contractDecl.putIfAbsent(key, top);
            var flags = contractParamUsed.computeIfAbsent(
                    key, k -> new boolean[top.getParameters().size()]);
            for (int i = 0; i < implParams.size() && i < flags.length; i++) {
                if (usedInEnclosingBody(index, impl, implParams.get(i).getSimpleName())) flags[i] = true;
            }
        }
    }

    private boolean usedInEnclosingBody(ModelIndex index, CtElement element, String name) {
        CtElement scope = element;
        while (scope != null && !(scope instanceof CtExecutable<?>) && !(scope instanceof CtBodyHolder)) {
            if (!scope.isParentInitialized()) return false;
            scope = scope.getParent();
        }
        if (scope == null) return false;
        var scopeRoot = scope;
        var pos = scopeRoot.getPosition();
        Set<String> names;
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) {
            names = scopeAccessNames(index, scopeRoot);
        } else {
            var key = pos.getFile().getPath() + "#" + pos.getSourceStart();
            names = accessNamesByScope.computeIfAbsent(key, k -> scopeAccessNames(index, scopeRoot));
        }
        return names.contains(name);
    }

    private static Set<String> scopeAccessNames(ModelIndex index, CtElement scope) {
        var out = new HashSet<String>();
        CtElement owner = scope;
        while (owner != null
                && !(owner instanceof CtMethod)
                && !(owner instanceof CtConstructor)
                && !(owner instanceof CtAnonymousExecutable)) {
            owner = owner.isParentInitialized() ? owner.getParent() : null;
        }
        if (owner == null) return out;
        var scopeIsOwner =
                scope instanceof CtMethod || scope instanceof CtConstructor || scope instanceof CtAnonymousExecutable;
        var spos = scope.getPosition();
        for (CtVariableAccess<?> access : index.accessesOf(owner).variableAccesses) {
            var ref = access.getVariable();
            if (ref == null) continue;
            if (!ModelIndex.isRead(access)) continue;
            if (!scopeIsOwner) {
                var apos = access.getPosition();
                if (apos != null
                        && apos.isValidPosition()
                        && spos != null
                        && spos.isValidPosition()
                        && (apos.getSourceStart() < spos.getSourceStart()
                                || apos.getSourceStart() > spos.getSourceEnd())) {
                    continue;
                }
            }
            out.add(ref.getSimpleName());
        }
        return out;
    }

    private String ownerName(CtMethod<?> m) {
        var t = m.getDeclaringType();
        return t == null ? "?" : t.getSimpleName();
    }

    private void report(String check, CtElement e, String message) {
        var pos = e.getPosition();
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) return;
        File f = pos.getFile();
        String abs = f.toPath().toAbsolutePath().normalize().toString();
        if (generatedFiles.contains(abs)) return;
        if (!seen.add(check + "|" + f.getPath() + "|" + pos.getLine() + "|" + pos.getColumn())) return;
        findings.add(new ConventionChecks.Finding(check, f.getPath(), pos.getLine(), message));
    }
}
