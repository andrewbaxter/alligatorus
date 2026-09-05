package compilerxyz.lint.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtRecordComponent;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class StateChecks {

    private static final String OPT = "compilerxyz.prelude.util.Opt";
    private static final int STATE_A = 1;
    private static final int STATE_B = 2;
    private static final int BOTH = STATE_A | STATE_B;

    private enum Kind {
        OPT,
        BOOL
    }

    private final List<ConventionChecks.Finding> findings = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> generatedFiles;
    private final Map<String, int[]> paramFlows = new HashMap<>();
    private final Map<String, Integer> fieldFlows = new HashMap<>();

    public StateChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<ConventionChecks.Finding> run(ModelIndex index) {
        Set<String> dynMethodNames = new HashSet<>();
        for (CtConstructorCall<?> cc : index.constructorCalls) {
            String ctor = cc.getType() == null ? "" : cc.getType().getSimpleName();
            int idx;
            switch (ctor) {
                case "TypeMethodDynamic" -> idx = 4;
                case "TypeFunctionConst" -> idx = 3;
                default -> {
                    continue;
                }
            }
            List<CtExpression<?>> args = cc.getArguments();
            if (args.size() > idx && args.get(idx) instanceof CtLiteral<?> lit && lit.getValue() instanceof String n) {
                dynMethodNames.add(n);
            }
        }
        for (CtInvocation<?> inv : index.invocations) {
            String dynName =
                    inv.getExecutable() == null ? "" : inv.getExecutable().getSimpleName();
            if (!"callStaticMethod".equals(dynName) && !"callStaticInterfaceMethod".equals(dynName)) {
                continue;
            }
            List<CtExpression<?>> dynArgs = inv.getArguments();
            if (dynArgs.size() > 2
                    && dynArgs.get(2) instanceof CtLiteral<?> lit
                    && lit.getValue() instanceof String n) {
                dynMethodNames.add(n);
            }
        }

        for (CtInvocation<?> inv : index.invocations) {
            recordCall(
                    inv.getExecutable() == null ? null : inv.getExecutable().getExecutableDeclaration(),
                    inv.getArguments());
        }
        for (CtConstructorCall<?> cc : index.constructorCalls) {
            recordCall(
                    cc.getExecutable() == null ? null : cc.getExecutable().getExecutableDeclaration(),
                    cc.getArguments());
        }
        for (CtExecutableReferenceExpression<?, ?> mr : index.executableRefExpressions) {
            CtExecutable<?> decl =
                    mr.getExecutable() == null ? null : mr.getExecutable().getExecutableDeclaration();
            for (CtExecutable<?> target : targetsOf(decl)) {
                int[] flows = flowsOf(target);
                List<CtParameter<?>> params = target.getParameters();
                for (int i = 0; i < params.size() && i < flows.length; i++) {
                    if (kindOf(params.get(i).getType()) != null) flows[i] |= BOTH;
                }
            }
        }

        for (CtField<?> f : index.fields) {
            Kind kind = kindOf(f.getType());
            if (kind == null) continue;
            if (f.getDefaultExpression() != null) {
                fieldFlows.merge(fieldKey(f), classify(kind, f.getDefaultExpression(), Map.of(), 0), (a, b) -> a | b);
            } else if (kind == Kind.BOOL && f.getType() != null && f.getType().isPrimitive()) {
                fieldFlows.merge(fieldKey(f), STATE_B, (a, b) -> a | b);
            }
        }
        for (CtAssignment<?, ?> a : index.assignments) {
            if (!(a.getAssigned() instanceof CtFieldWrite<?> fw) || fw.getVariable() == null) continue;
            CtField<?> f = fw.getVariable().getFieldDeclaration();
            if (f == null) continue;
            Kind kind = kindOf(f.getType());
            if (kind == null) continue;
            fieldFlows.merge(fieldKey(f), classify(kind, a.getAssignment(), Map.of(), 0), (x, y) -> x | y);
        }

        for (CtType<?> t : index.types) {
            List<CtExecutable<?>> execs = new ArrayList<>(t.getMethods());
            if (t instanceof CtClass<?> c) execs.addAll(c.getConstructors());
            for (CtExecutable<?> ex : execs) {
                List<CtParameter<?>> params = ex.getParameters();
                boolean anyTracked = false;
                for (CtParameter<?> p : params) anyTracked |= kindOf(p.getType()) != null;
                if (!anyTracked) continue;
                if (ex instanceof CtMethod<?> m) {
                    if (!m.getTopDefinitions().isEmpty()) continue;
                    if (dynMethodNames.contains(m.getSimpleName())) continue;
                }
                boolean descriptor = ex.getAnnotations().stream()
                        .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("CodegenDescriptor"));
                if (descriptor) continue;
                int[] flows = paramFlows.get(key(ex));
                for (int i = 0; i < params.size(); i++) {
                    Kind kind = kindOf(params.get(i).getType());
                    if (kind == null) continue;
                    int mask = flows == null || i >= flows.length ? 0 : flows[i];
                    if (mask == 0 || mask == BOTH) continue;
                    String where = key(ex);
                    String name = params.get(i).getSimpleName();
                    CtElement at = params.get(i);
                    if (!valid(at) && t instanceof CtRecord rec) {
                        for (CtRecordComponent rc : rec.getRecordComponents()) {
                            if (rc.getSimpleName().equals(name) && valid(rc)) at = rc;
                        }
                    }
                    if (!valid(at)) at = valid(ex) ? ex : t;
                    String message;
                    if (kind == Kind.OPT) {
                        message = mask == STATE_A
                                ? "Opt parameter '" + name + "' of '" + where
                                        + "' is Some at every call site; make it non-optional."
                                : "Opt parameter '" + name + "' of '" + where
                                        + "' is None at every call site; remove the parameter.";
                    } else {
                        message = "boolean parameter '" + name + "' of '" + where + "' is "
                                + (mask == STATE_A ? "true" : "false")
                                + " at every call site; remove the parameter.";
                    }
                    report(kind == Kind.OPT ? "OptParameterBothStates" : "BoolParameterBothStates", at, message);
                }
            }
        }
        for (CtField<?> f : index.fields) {
            Kind kind = kindOf(f.getType());
            if (kind == null) continue;
            Integer mask = fieldFlows.get(fieldKey(f));
            if (mask == null || mask == BOTH) continue;
            String message;
            if (kind == Kind.OPT) {
                message = mask == STATE_A
                        ? "Opt field '" + fieldKey(f) + "' is only ever assigned Some values; make it non-optional."
                        : "Opt field '" + fieldKey(f) + "' is only ever assigned None; remove the field.";
            } else {
                message = "boolean field '" + fieldKey(f) + "' is only ever " + (mask == STATE_A ? "true" : "false")
                        + "; remove the field.";
            }
            report(kind == Kind.OPT ? "OptFieldBothStates" : "BoolFieldBothStates", f, message);
        }
        return findings;
    }

    private void recordCall(CtExecutable<?> decl, List<CtExpression<?>> args) {
        for (CtExecutable<?> target : targetsOf(decl)) {
            List<CtParameter<?>> params = target.getParameters();
            boolean anyTracked = false;
            for (CtParameter<?> p : params) anyTracked |= kindOf(p.getType()) != null;
            if (!anyTracked) continue;
            int[] flows = flowsOf(target);
            int n = Math.min(params.size(), args.size());
            for (int i = 0; i < n && i < flows.length; i++) {
                Kind kind = kindOf(params.get(i).getType());
                if (kind == null) continue;
                flows[i] |= classify(kind, args.get(i), Map.of(), 0);
            }
        }
    }

    private int classify(Kind kind, CtExpression<?> e, Map<String, Integer> env, int depth) {
        if (e == null || depth > 16) return BOTH;
        if (e instanceof CtLiteral<?> lit) {
            if (kind == Kind.BOOL && lit.getValue() instanceof Boolean b) {
                return b ? STATE_A : STATE_B;
            }
            return BOTH;
        }
        if (e instanceof CtUnaryOperator<?> u) {
            if (kind != Kind.BOOL || u.getKind() != UnaryOperatorKind.NOT) return BOTH;
            int inner = classify(kind, u.getOperand(), env, depth + 1);
            return ((inner & STATE_A) != 0 ? STATE_B : 0) | ((inner & STATE_B) != 0 ? STATE_A : 0);
        }
        if (e instanceof CtConditional<?> c) {
            return classify(kind, c.getThenExpression(), env, depth + 1)
                    | classify(kind, c.getElseExpression(), env, depth + 1);
        }
        if (e instanceof CtInvocation<?> inv) {
            var ex = inv.getExecutable();
            String name = ex == null ? "" : ex.getSimpleName();
            String owner = ex == null || ex.getDeclaringType() == null
                    ? ""
                    : ex.getDeclaringType().getQualifiedName();
            if (kind == Kind.OPT && OPT.equals(owner)) {
                if ("none".equals(name)) return STATE_B;
                if ("some".equals(name)) return STATE_A;
            }
            if (ex != null
                    && ex.getExecutableDeclaration() instanceof CtExecutable<?> md
                    && md.getBody() != null
                    && md.getBody().getStatements().size() == 1
                    && md.getBody().getStatements().get(0) instanceof CtReturn<?> rtn
                    && rtn.getReturnedExpression() != null
                    && md.getParameters().size() == inv.getArguments().size()) {
                Map<String, Integer> callEnv = new HashMap<>();
                for (int i = 0; i < md.getParameters().size(); i++) {
                    callEnv.put(
                            md.getParameters().get(i).getSimpleName(),
                            classify(kind, inv.getArguments().get(i), env, depth + 1));
                }
                return classify(kind, rtn.getReturnedExpression(), callEnv, depth + 1);
            }
            return BOTH;
        }
        if (e instanceof CtConstructorCall<?> cc) {
            if (kind != Kind.OPT) return BOTH;
            String qn = cc.getType() == null ? "" : cc.getType().getQualifiedName();
            if ((OPT + "$Some").equals(qn)) return STATE_A;
            if ((OPT + "$None").equals(qn)) return STATE_B;
            return BOTH;
        }
        if (e instanceof CtVariableRead<?> vr && vr.getVariable() != null) {
            var decl = vr.getVariable().getDeclaration();
            if (decl instanceof CtParameter<?> p) {
                Integer bound = env.get(p.getSimpleName());
                return bound == null ? BOTH : bound;
            }
            if (decl instanceof CtLocalVariable<?> lv) {
                int mask = 0;
                if (lv.getDefaultExpression() != null) {
                    mask |= classify(kind, lv.getDefaultExpression(), env, depth + 1);
                }
                CtExecutable<?> body = lv.getParent(CtExecutable.class);
                if (body != null) {
                    for (CtAssignment<?, ?> w : body.getElements(new TypeFilter<>(CtAssignment.class))) {
                        if (w.getAssigned() instanceof CtVariableWrite<?> vw
                                && vw.getVariable() != null
                                && vw.getVariable().getSimpleName().equals(lv.getSimpleName())) {
                            mask |= classify(kind, w.getAssignment(), env, depth + 1);
                        }
                    }
                }
                return mask == 0 ? BOTH : mask;
            }
            return BOTH;
        }
        if (e instanceof CtFieldRead<?> fr && fr.getVariable() != null) {
            CtField<?> ff = fr.getVariable().getFieldDeclaration();
            if (ff != null && ff.hasModifier(ModifierKind.FINAL) && ff.getDefaultExpression() != null) {
                return classify(kind, ff.getDefaultExpression(), Map.of(), depth + 1);
            }
            return BOTH;
        }
        return BOTH;
    }

    private int[] flowsOf(CtExecutable<?> target) {
        return paramFlows.computeIfAbsent(
                key(target), x -> new int[target.getParameters().size()]);
    }

    private static List<CtExecutable<?>> targetsOf(CtExecutable<?> decl) {
        if (decl == null) return List.of();
        List<CtExecutable<?>> out = new ArrayList<>();
        out.add(decl);
        if (decl instanceof CtMethod<?> m) out.addAll(m.getTopDefinitions());
        return out;
    }

    private static Kind kindOf(CtTypeReference<?> ref) {
        if (ref == null) return null;
        String qn = ref.getQualifiedName();
        if (OPT.equals(qn)) return Kind.OPT;
        if ("boolean".equals(qn) || "java.lang.Boolean".equals(qn)) return Kind.BOOL;
        return null;
    }

    private static boolean valid(CtElement e) {
        var pos = e.getPosition();
        return pos != null && pos.isValidPosition() && pos.getFile() != null;
    }

    private static String key(CtExecutable<?> ex) {
        String owner = ex instanceof CtTypeMember tm && tm.getDeclaringType() != null
                ? tm.getDeclaringType().getQualifiedName()
                : "?";
        StringBuilder sb =
                new StringBuilder(owner).append('#').append(ex.getSimpleName()).append('(');
        List<CtParameter<?>> ps = ex.getParameters();
        for (int i = 0; i < ps.size(); i++) {
            if (i > 0) sb.append(',');
            var ty = ps.get(i).getType();
            sb.append(ty == null ? "?" : ty.getSimpleName());
        }
        return sb.append(')').toString();
    }

    private static String fieldKey(CtField<?> f) {
        String owner = f.getDeclaringType() != null ? f.getDeclaringType().getQualifiedName() : "?";
        return owner + "." + f.getSimpleName();
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
