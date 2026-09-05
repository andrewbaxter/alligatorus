package compilerxyz.lint.reach;

import compilerxyz.lint.convention.JavaNames;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtRecordComponent;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;

public final class NamingChecks {

    private final List<ConventionChecks.Finding> findings = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> generatedFiles;
    private final JavaNames names = new JavaNames();

    public NamingChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<ConventionChecks.Finding> run(ModelIndex index) {
        for (CtType<?> t : index.types) {
            if (t.isImplicit() || t.isAnonymous()) continue;
            if (t instanceof CtTypeParameter) {
                check(
                        "NamingTypeParameter",
                        t,
                        t.getSimpleName(),
                        JavaNames.UPPER_CAMEL,
                        "type parameter",
                        "UpperCamelCase");
                continue;
            }
            String kind = t instanceof CtInterface<?>
                    ? "interface"
                    : t.isEnum() ? "enum" : t instanceof CtRecord ? "record" : "class";
            String typeName = t.isLocalType() ? t.getSimpleName().replaceFirst("^\\d+", "") : t.getSimpleName();
            check("NamingType", t, typeName, JavaNames.UPPER_CAMEL, kind, "UpperCamelCase");
            if (t instanceof CtRecord rec) {
                for (CtRecordComponent rc : rec.getRecordComponents()) {
                    check(
                            "NamingField",
                            rec,
                            rc.getSimpleName(),
                            JavaNames.LOWER_CAMEL,
                            "record component",
                            "lowerCamelCase");
                }
            }
            if (t.isTopLevel()) {
                var pos = t.getPosition();
                if (pos != null && pos.isValidPosition() && pos.getFile() != null) {
                    String fileName = pos.getFile().getName();
                    if (!fileName.equals("package-info.java") && !fileName.equals(typeName + ".java")) {
                        report(
                                "NamingFileType",
                                t,
                                "top-level " + kind + " '" + t.getSimpleName() + "' must live in its own file named '"
                                        + t.getSimpleName() + ".java' (found in '" + fileName + "').");
                    }
                }
            }
        }
        for (CtMethod<?> m : index.methods) {
            if (m.isImplicit()) continue;
            check("NamingMethod", m, m.getSimpleName(), JavaNames.LOWER_CAMEL, "method", "lowerCamelCase");
            checkParameters(m.getParameters());
        }
        for (CtConstructor<?> c : index.constructors) {
            if (c.isImplicit()) continue;
            checkParameters(c.getParameters());
        }
        for (CtLambda<?> l : index.lambdas) {
            checkParameters(l.getParameters());
        }
        for (CtField<?> f : index.fields) {
            if (f.isImplicit()) continue;
            if (f instanceof CtEnumValue<?>) {
                check(
                        "NamingEnumConstant",
                        f,
                        f.getSimpleName(),
                        JavaNames.UPPER_SNAKE,
                        "enum constant",
                        "UPPER_SNAKE_CASE");
                continue;
            }
            boolean constant = (f.hasModifier(ModifierKind.STATIC) && f.hasModifier(ModifierKind.FINAL))
                    || f.getDeclaringType() instanceof CtInterface<?>;
            if (constant) {
                if (f.getSimpleName().equals("serialVersionUID")) continue;
                check(
                        "NamingConstant",
                        f,
                        f.getSimpleName(),
                        JavaNames.UPPER_SNAKE,
                        "static final field",
                        "UPPER_SNAKE_CASE");
            } else {
                check("NamingField", f, f.getSimpleName(), JavaNames.LOWER_CAMEL, "field", "lowerCamelCase");
            }
        }
        for (CtLocalVariable<?> lv : index.localVariables) {
            if (lv.isImplicit()) continue;
            check("NamingLocal", lv, lv.getSimpleName(), JavaNames.LOWER_CAMEL, "local variable", "lowerCamelCase");
        }
        for (CtCatchVariable<?> cv : index.catchVariables) {
            if (cv.isImplicit()) continue;
            check("NamingLocal", cv, cv.getSimpleName(), JavaNames.LOWER_CAMEL, "catch parameter", "lowerCamelCase");
        }
        return findings;
    }

    private void checkParameters(List<? extends CtParameter<?>> params) {
        for (CtParameter<?> p : params) {
            if (p.isImplicit()) continue;
            check("NamingParameter", p, p.getSimpleName(), JavaNames.LOWER_CAMEL, "parameter", "lowerCamelCase");
        }
    }

    private void check(String checkName, CtElement e, String name, Pattern pattern, String what, String style) {
        String bare = names.bare(name);
        if (pattern.matcher(bare).matches()) return;
        String rule = bare.equals(name) && (name.startsWith("_") || name.endsWith("_"))
                ? JavaNames.UNDERSCORE_RULE
                : "must be " + style;
        report(checkName, e, what + " '" + name + "': " + rule + ".");
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
