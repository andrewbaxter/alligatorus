package compilerxyz.lint.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public final class TypecheckConvertChecks {
    private static final String EVAL = "compilerxyz.main.eval.Eval";
    private static final String METHOD = "typecheckConvert";

    private final List<ConventionChecks.Finding> findings = new ArrayList<>();
    private final Set<String> generatedFiles;

    public TypecheckConvertChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<ConventionChecks.Finding> run(ModelIndex index) {
        for (CtType<?> t : index.types) {
            if (!EVAL.equals(t.getQualifiedName())) continue;
            for (CtMethod<?> m : t.getMethods()) {
                if (!METHOD.equals(m.getSimpleName()) || m.getBody() == null) continue;
                for (CtInvocation<?> inv : m.getBody().getElements(new TypeFilter<>(CtInvocation.class))) {
                    var exec = inv.getExecutable();
                    if (exec == null) continue;
                    String name = exec.getSimpleName();
                    if (!name.equals("typeIsEqual") && !name.equals("equals")) continue;
                    var pos = inv.getPosition();
                    if (pos == null || !pos.isValidPosition() || pos.getFile() == null) continue;
                    File f = pos.getFile();
                    if (generatedFiles.contains(
                            f.toPath().toAbsolutePath().normalize().toString())) continue;
                    findings.add(new ConventionChecks.Finding(
                            "TypecheckConvertShortCircuit",
                            f.getPath(),
                            pos.getLine(),
                            "Eval.typecheckConvert calls '" + name + "'; it must be castTo, else castFrom, else"
                                    + " error — never a type-equality short-circuit."));
                }
            }
        }
        return findings;
    }
}
