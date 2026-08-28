package compilerxyz.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class InheritanceChecks {

    private final Set<String> mandatedSuperclasses = Set.of(
            "java.lang.Throwable",
            "java.lang.Exception",
            "java.lang.RuntimeException",
            "java.lang.Error",
            "java.lang.ClassLoader",
            "org.objectweb.asm.ClassWriter");

    private final List<ConventionChecks.Finding> findings = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> generatedFiles;

    public InheritanceChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<ConventionChecks.Finding> run(CtModel model) {
        for (CtClass<?> c : model.getElements(new TypeFilter<>(CtClass.class))) {
            if (c.hasModifier(ModifierKind.ABSTRACT) && !c.isEnum()) {
                report(
                        "NoClassInheritance",
                        c,
                        "abstract class '" + c.getQualifiedName()
                                + "' exists only to be subclassed; compose shared behavior via delegation and"
                                + " shared utility methods instead.");
            }
            CtTypeReference<?> sc = c.getSuperclass();
            if (sc == null) continue;
            String qn = sc.getQualifiedName();
            if (qn == null
                    || "java.lang.Object".equals(qn)
                    || "java.lang.Enum".equals(qn)
                    || "java.lang.Record".equals(qn)) {
                continue;
            }
            if (mandatedSuperclasses.contains(qn)) continue;
            CtType<?> scDecl = sc.getTypeDeclaration();
            if (scDecl != null && scDecl.isEnum()) continue;
            report(
                    "NoClassInheritance",
                    c,
                    "'" + c.getQualifiedName() + "' extends '" + qn
                            + "'; class inheritance is banned — compose via delegation and shared utility"
                            + " methods.");
        }
        for (CtInterface<?> i : model.getElements(new TypeFilter<>(CtInterface.class))) {
            for (CtMethod<?> m : i.getMethods()) {
                if (!m.isDefaultMethod()) continue;
                report(
                        "NoInterfaceDefaultMethod",
                        m,
                        "'" + i.getSimpleName() + "." + m.getSimpleName()
                                + "' is a default method; interfaces declare contracts only — move the body to a"
                                + " static utility method.");
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
