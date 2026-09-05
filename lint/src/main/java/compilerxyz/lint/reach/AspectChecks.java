package compilerxyz.lint.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.reference.CtTypeReference;

public final class AspectChecks {
    private static final String ASPECT = "compilerxyz.main.eval.aspect.Aspect";

    private final List<ConventionChecks.Finding> findings = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> generatedFiles;

    public AspectChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<ConventionChecks.Finding> run(ModelIndex index) {
        for (CtType<?> t : index.types) {
            if (t instanceof CtTypeParameter || t.isImplicit()) continue;
            var aspectRef = t.getFactory().Type().createReference(ASPECT);
            var aspects = new ArrayList<String>();
            var others = new ArrayList<String>();
            for (CtTypeReference<?> si : t.getSuperInterfaces()) {
                String qn = si.getQualifiedName();
                if (ASPECT.equals(qn)) continue;
                if (si.isSubtypeOf(aspectRef)) aspects.add(si.getSimpleName());
                else others.add(si.getSimpleName());
            }
            if (aspects.isEmpty()) continue;
            String name = t.isAnonymous() ? "anonymous class" : "'" + t.getQualifiedName() + "'";
            if (t instanceof CtInterface<?>) {
                if (!others.isEmpty()) {
                    report(
                            "AspectInterfaceMixed",
                            t,
                            "aspect interface " + name + " extends " + aspects + " together with non-aspect " + others
                                    + "; an aspect interface may only extend other aspect interfaces.");
                }
                continue;
            }
            if (t instanceof CtClass<?> c
                    && c.getSuperclass() != null
                    && !"java.lang.Object".equals(c.getSuperclass().getQualifiedName())
                    && !"java.lang.Record".equals(c.getSuperclass().getQualifiedName())
                    && !"java.lang.Enum".equals(c.getSuperclass().getQualifiedName())) {
                others.add(c.getSuperclass().getSimpleName());
            }
            if (!others.isEmpty()) {
                report(
                        "AspectImplStandalone",
                        t,
                        name + " implements aspect " + aspects + " alongside " + others
                                + "; aspect implementations must be standalone classes (implementing nothing else)"
                                + " returned from typeAspect — the aspect may hold a reference to the type.");
            }
            if (aspects.size() > 1) {
                report(
                        "AspectImplSingle",
                        t,
                        name + " implements " + aspects.size() + " aspects " + aspects
                                + "; each aspect implementation must be its own class.");
            }
        }
        return findings;
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
