package compilerxyz.reach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ReachabilityMain {

    private final Set<String> generatedFiles;

    public ReachabilityMain(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    private final Set<String> junit = Set.of(
            "Test",
            "RepeatedTest",
            "TestFactory",
            "ParameterizedTest",
            "BeforeEach",
            "AfterEach",
            "BeforeAll",
            "AfterAll");
    private static final String CODEGEN_DESCRIPTOR = "CodegenDescriptor";

    private final Map<String, CtExecutable<?>> execByKey = new HashMap<>();
    private final Map<String, List<String>> keysByNameArity = new HashMap<>();
    private final Set<String> reachable = new HashSet<>();
    private final Map<String, Integer> callCounts = new HashMap<>();
    private final Map<String, Integer> fieldReads = new HashMap<>();
    private final Map<String, Integer> fieldWrites = new HashMap<>();
    private final Set<String> referencedTypes = new HashSet<>();
    private final Set<String> genReferenced = new HashSet<>();

    public int run(CtModel model) throws IOException {
        List<CtType<?>> types = new ArrayList<>();
        for (CtType<?> t : model.getAllTypes()) {
            collectTypes(t, types);
        }
        List<CtType<?>> codegenTypes = new ArrayList<>();
        types.removeIf(t -> {
            String qn = t.getQualifiedName();
            boolean cg = qn == null || qn.startsWith("compilerxyz.codegen.");
            if (cg) codegenTypes.add(t);
            return cg;
        });
        for (CtType<?> t : types) {
            for (CtExecutable<?> ex : executablesOf(t)) {
                String k = key(ex);
                execByKey.put(k, ex);
                keysByNameArity
                        .computeIfAbsent(
                                nameArity(ex.getSimpleName(), ex.getParameters().size()), x -> new ArrayList<>())
                        .add(k);
            }
        }
        Deque<String> work = new ArrayDeque<>();
        for (CtType<?> t : types) {
            for (CtExecutable<?> ex : executablesOf(t)) {
                if (isRoot(ex) || (ex instanceof CtMethod<?> me && me.getBody() != null && overridesExternal(me))) {
                    String k = key(ex);
                    if (reachable.add(k)) work.add(k);
                }
            }
        }
        Set<String> dynMethodNames = new HashSet<>();
        for (CtType<?> t : types) {
            for (CtConstructorCall<?> cc : t.getElements(new TypeFilter<>(CtConstructorCall.class))) {
                String ctorType = cc.getType().getSimpleName();
                int nameIdx;
                switch (ctorType) {
                    case "TypeMethodDynamic" -> nameIdx = 4;
                    case "TypeFunctionDynamic" -> nameIdx = 3;
                    default -> {
                        continue;
                    }
                }
                List<CtExpression<?>> args = cc.getArguments();
                if (args.size() > nameIdx
                        && args.get(nameIdx) instanceof CtLiteral<?> lit
                        && lit.getValue() instanceof String n) {
                    dynMethodNames.add(n);
                }
            }
            for (CtInvocation<?> inv : t.getElements(new TypeFilter<>(CtInvocation.class))) {
                String invName = inv.getExecutable().getSimpleName();
                if (!"callStaticMethod".equals(invName) && !"callStaticInterfaceMethod".equals(invName)) continue;
                List<CtExpression<?>> args = inv.getArguments();
                if (args.size() > 2 && args.get(2) instanceof CtLiteral<?> lit && lit.getValue() instanceof String n) {
                    dynMethodNames.add(n);
                }
            }
        }
        for (CtType<?> t : types) {
            for (CtExecutable<?> ex : executablesOf(t)) {
                if (ex instanceof CtMethod<?> m && dynMethodNames.contains(m.getSimpleName())) {
                    String k = key(ex);
                    genReferenced.add(k);
                    if (reachable.add(k)) work.add(k);
                }
            }
        }
        for (CtType<?> t : codegenTypes) {
            for (CtField<?> f : t.getFields()) {
                var init = f.getDefaultExpression();
                if (init == null) continue;
                for (CtInvocation<?> inv : init.getElements(new TypeFilter<>(CtInvocation.class))) {
                    onCall(inv.getExecutable(), inv.getArguments().size(), work, false);
                }
                for (CtConstructorCall<?> cc : init.getElements(new TypeFilter<>(CtConstructorCall.class))) {
                    onCall(cc.getExecutable(), cc.getArguments().size(), work, false);
                }
            }
            if (t instanceof CtClass<?> cgClass) {
                for (CtAnonymousExecutable ab : cgClass.getAnonymousExecutables()) {
                    for (CtInvocation<?> inv : ab.getElements(new TypeFilter<>(CtInvocation.class))) {
                        onCall(inv.getExecutable(), inv.getArguments().size(), work, false);
                    }
                    for (CtConstructorCall<?> cc : ab.getElements(new TypeFilter<>(CtConstructorCall.class))) {
                        onCall(cc.getExecutable(), cc.getArguments().size(), work, false);
                    }
                }
            }
            for (CtExecutable<?> ex : executablesOf(t)) {
                for (CtInvocation<?> inv : ex.getElements(new TypeFilter<>(CtInvocation.class))) {
                    onCall(inv.getExecutable(), inv.getArguments().size(), work, false);
                }
                for (CtConstructorCall<?> cc : ex.getElements(new TypeFilter<>(CtConstructorCall.class))) {
                    onCall(cc.getExecutable(), cc.getArguments().size(), work, false);
                }
                for (CtFieldRead<?> fr : ex.getElements(new TypeFilter<>(CtFieldRead.class))) {
                    if (fr.getVariable() != null) {
                        fieldReads.merge(fieldKey(fr.getVariable().getQualifiedName()), 1, Integer::sum);
                    }
                }
                for (CtFieldWrite<?> fw : ex.getElements(new TypeFilter<>(CtFieldWrite.class))) {
                    if (fw.getVariable() != null) {
                        fieldWrites.merge(fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                    }
                }
            }
            for (CtTypeReference<?> tr : t.getElements(new TypeFilter<>(CtTypeReference.class))) {
                String cgQn = tr.getQualifiedName();
                if (cgQn != null && !cgQn.isEmpty()) referencedTypes.add(cgQn);
            }
        }
        Set<String> fieldInitDone = new HashSet<>();
        while (!work.isEmpty()) {
            String workKey = work.poll();
            CtExecutable<?> ex = execByKey.get(workKey);
            if (ex == null) continue;
            boolean fromGenerated = ex instanceof CtTypeMember exTm
                    && exTm.getDeclaringType() != null
                    && isGeneratedType(exTm.getDeclaringType());
            if (ex instanceof CtTypeMember tm && tm.getDeclaringType() != null) {
                String tn = tm.getDeclaringType().getQualifiedName();
                if (fieldInitDone.add(tn)) {
                    for (CtField<?> f : tm.getDeclaringType().getFields()) {
                        var init = f.getDefaultExpression();
                        if (init == null) continue;
                        for (CtInvocation<?> inv : init.getElements(new TypeFilter<>(CtInvocation.class))) {
                            onCall(inv.getExecutable(), inv.getArguments().size(), work, fromGenerated);
                        }
                        for (CtConstructorCall<?> cc : init.getElements(new TypeFilter<>(CtConstructorCall.class))) {
                            onCall(cc.getExecutable(), cc.getArguments().size(), work, fromGenerated);
                        }
                    }
                }
            }
            for (CtTypeReference<?> _artTr : ex.getElements(new TypeFilter<>(CtTypeReference.class))) {
                String _artQn = _artTr.getQualifiedName();
                if (_artQn != null && !_artQn.isEmpty()) referencedTypes.add(_artQn);
            }
            for (CtInvocation<?> inv : ex.getElements(new TypeFilter<>(CtInvocation.class))) {
                onCall(inv.getExecutable(), inv.getArguments().size(), work, fromGenerated);
            }
            for (CtConstructorCall<?> cc : ex.getElements(new TypeFilter<>(CtConstructorCall.class))) {
                onCall(cc.getExecutable(), cc.getArguments().size(), work, fromGenerated);
            }
            for (CtFieldRead<?> fr : ex.getElements(new TypeFilter<>(CtFieldRead.class))) {
                if (fr.getVariable() != null) {
                    fieldReads.merge(fieldKey(fr.getVariable().getQualifiedName()), 1, Integer::sum);
                }
            }
            for (CtFieldWrite<?> fw : ex.getElements(new TypeFilter<>(CtFieldWrite.class))) {
                if (fw.getVariable() != null) {
                    fieldWrites.merge(fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                }
            }
        }
        for (CtType<?> t : types) {
            boolean live = executablesOf(t).stream().anyMatch(e -> reachable.contains(key(e)));
            if (live) {
                for (CtTypeReference<?> tr : t.getElements(new TypeFilter<>(CtTypeReference.class))) {
                    String qn = tr.getQualifiedName();
                    if (qn != null && !qn.isEmpty()) referencedTypes.add(qn);
                }
            }
        }

        List<String> deadClasses = new ArrayList<>();
        List<String> deadMethods = new ArrayList<>();
        List<String> deadFields = new ArrayList<>();
        List<String> singleUseMethods = new ArrayList<>();
        List<String> singleUseFields = new ArrayList<>();

        for (CtType<?> t : types) {
            String qn = t.getQualifiedName();
            List<CtExecutable<?>> execs = executablesOf(t);
            boolean anyReachable = execs.stream().anyMatch(e -> reachable.contains(key(e)));
            boolean rooted = isGeneratedType(t);
            boolean isAnnotation = t.isAnnotationType();
            if (!anyReachable && !rooted && !isAnnotation && !referencedTypes.contains(qn)) {
                deadClasses.add(qn);
                continue;
            }
            for (CtExecutable<?> ex : execs) {
                if (!(ex instanceof CtMethod<?> m)) continue;
                String k = key(ex);
                boolean concrete = m.getBody() != null;
                boolean _iam = false;
                for (CtMethod<?> _iamTop : m.getTopDefinitions()) {
                    if (!key(_iamTop).equals(key(m)) && _iamTop.isAbstract()) {
                        _iam = true;
                        break;
                    }
                }
                boolean inlinable = !isRoot(ex)
                        && !rooted
                        && !"compilerxyz.util.Assert".equals(qn)
                        && m.getBody() != null
                        && !overridesExternal(m)
                        && !_iam
                        && !isRecordAccessor(t, m)
                        && (m.hasModifier(ModifierKind.PRIVATE) || m.hasModifier(ModifierKind.STATIC));
                if (!reachable.contains(k)
                        && concrete
                        && !rooted
                        && !isRoot(ex)
                        && !m.getSimpleName().equals("main")
                        && !overridesExternal(m)
                        && !isRecordAccessor(t, m)) {
                    deadMethods.add(qn + "#" + sig(ex));
                }
                if (inlinable
                        && reachable.contains(k)
                        && !genReferenced.contains(k)
                        && callCounts.getOrDefault(k, 0) == 1) {
                    singleUseMethods.add(qn + "#" + sig(ex));
                }
            }
            boolean recordOrEnum = t.isEnum() || (t instanceof CtRecord);
            if (!recordOrEnum) {
                for (CtField<?> f : t.getFields()) {
                    if (f.getModifiers().contains(ModifierKind.PRIVATE)
                            && !rooted
                            && !f.getSimpleName().equals("serialVersionUID")
                            && fieldWrites.getOrDefault(
                                            fieldKey(f.getReference().getQualifiedName()), 0)
                                    == 0
                            && fieldReads.getOrDefault(fieldKey(f.getReference().getQualifiedName()), 0) == 0) {
                        deadFields.add(qn + "#" + f.getSimpleName());
                    }
                    if (f.getModifiers().contains(ModifierKind.PRIVATE)
                            && !f.getModifiers().contains(ModifierKind.FINAL)
                            && !f.getModifiers().contains(ModifierKind.VOLATILE)
                            && fieldWrites.getOrDefault(
                                            fieldKey(f.getReference().getQualifiedName()), 0)
                                    == 0
                            && fieldReads.getOrDefault(fieldKey(f.getReference().getQualifiedName()), 0) == 1) {
                        singleUseFields.add(qn + "#" + f.getSimpleName());
                    }
                }
            }
        }

        deadClasses.sort(Comparator.naturalOrder());
        deadMethods.sort(Comparator.naturalOrder());
        singleUseMethods.sort(Comparator.naturalOrder());
        singleUseFields.sort(Comparator.naturalOrder());

        StringBuilder out = new StringBuilder();
        line(out, "=== Reachability / single-use analysis (Spoon, source-level) ===");
        line(out, "types: " + types.size());
        line(out, "");
        section(out, "#1 UNREACHABLE CLASSES (dead)", deadClasses);
        section(out, "#1 UNREACHABLE METHODS (dead)", deadMethods);
        section(out, "#2 SINGLE-USE METHODS (inline candidates: private/static, 1 caller)", singleUseMethods);
        section(out, "#1 DEAD PRIVATE FIELDS (0 reads)", deadFields);
        section(out, "#2 SINGLE-USE PRIVATE FIELDS (1 read)", singleUseFields);
        int total = deadClasses.size()
                + deadMethods.size()
                + deadFields.size()
                + singleUseMethods.size()
                + singleUseFields.size();
        line(
                out,
                String.format(
                        "TOT:  dead classes=%d  dead methods=%d  dead fields=%d  single-use methods=%d"
                                + "  single-use fields=%d",
                        deadClasses.size(),
                        deadMethods.size(),
                        deadFields.size(),
                        singleUseMethods.size(),
                        singleUseFields.size()));

        System.out.print(out);
        Path reportPath = Path.of("analysis/target/reachability-report.txt");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, out.toString());
        System.out.println("(report written to " + reportPath + ")");
        return total;
    }

    private void onCall(CtExecutableReference<?> ref, int actualArity, Deque<String> work, boolean fromGenerated) {
        if (ref == null) return;
        CtExecutable<?> decl = ref.getExecutableDeclaration();
        if (decl != null) {
            String dk = key(decl);
            callCounts.merge(dk, 1, Integer::sum);
            if (fromGenerated) genReferenced.add(dk);
            if (reachable.add(dk)) work.add(dk);
        }
        CtTypeReference<?> refDecl = ref.getDeclaringType();
        int refArity = ref.getParameters().size();
        Set<Integer> arities = new LinkedHashSet<>();
        arities.add(actualArity);
        if (refArity != actualArity) arities.add(refArity);
        for (int arity : arities) {
            for (String cand : keysByNameArity.getOrDefault(nameArity(ref.getSimpleName(), arity), List.of())) {
                CtExecutable<?> cex = execByKey.get(cand);
                if (cex == null) continue;
                if (isDirect(ref, cex) || subtypeOf(cex, refDecl)) {
                    if (reachable.add(cand)) work.add(cand);
                }
            }
        }
        if (actualArity != 1) {
            for (String cand : keysByNameArity.getOrDefault(nameArity(ref.getSimpleName(), 1), List.of())) {
                CtExecutable<?> cex = execByKey.get(cand);
                if (cex == null) continue;
                boolean isVarargs = !cex.getParameters().isEmpty()
                        && cex.getParameters()
                                .get(cex.getParameters().size() - 1)
                                .isVarArgs();
                if (!isVarargs) continue;
                if (isDirect(ref, cex) || subtypeOf(cex, refDecl)) {
                    if (reachable.add(cand)) work.add(cand);
                }
            }
        }
    }

    private static boolean isDirect(CtExecutableReference<?> ref, CtExecutable<?> cex) {
        CtExecutable<?> decl = ref.getExecutableDeclaration();
        return decl != null && key(cex).equals(key(decl));
    }

    private boolean subtypeOf(CtExecutable<?> cex, CtTypeReference<?> superRef) {
        if (superRef == null) return false;
        if (!(cex instanceof CtTypeMember tm) || tm.getDeclaringType() == null) return false;
        return tm.getDeclaringType().getReference().isSubtypeOf(superRef);
    }

    private boolean isRoot(CtExecutable<?> ex) {
        if (ex.getAnnotations().stream()
                .anyMatch(a -> junit.contains(a.getAnnotationType().getSimpleName())
                        || a.getAnnotationType().getSimpleName().equals(CODEGEN_DESCRIPTOR))) {
            return true;
        }
        return ex instanceof CtMethod<?> m
                && m.getSimpleName().equals("main")
                && m.getModifiers().contains(ModifierKind.STATIC)
                && m.getParameters().size() == 1;
    }

    private static void collectTypes(CtType<?> t, List<CtType<?>> out) {
        out.add(t);
        for (CtType<?> nested : t.getNestedTypes()) {
            collectTypes(nested, out);
        }
        for (CtField<?> f : t.getFields()) {
            var init = f.getDefaultExpression();
            if (init == null) continue;
            for (CtNewClass<?> nc : init.getElements(new TypeFilter<>(CtNewClass.class))) {
                var anon = nc.getAnonymousClass();
                if (anon != null) collectTypes(anon, out);
            }
        }
    }

    private static List<CtExecutable<?>> executablesOf(CtType<?> t) {
        List<CtExecutable<?>> out = new ArrayList<>();
        for (CtMethod<?> m : t.getMethods()) out.add(m);
        if (t instanceof CtClass<?> c) {
            for (CtConstructor<?> ctor : c.getConstructors()) out.add(ctor);
        }
        return out;
    }

    private static String key(CtExecutable<?> ex) {
        String owner = ex instanceof CtTypeMember tm && tm.getDeclaringType() != null
                ? tm.getDeclaringType().getQualifiedName()
                : "?";
        return owner + "#" + sig(ex);
    }

    private static String sig(CtExecutable<?> ex) {
        StringBuilder sb = new StringBuilder(ex.getSimpleName()).append('(');
        var ps = ex.getParameters();
        for (int i = 0; i < ps.size(); i++) {
            if (i > 0) sb.append(',');
            var ty = ps.get(i).getType();
            sb.append(ty == null ? "?" : ty.getSimpleName());
        }
        return sb.append(')').toString();
    }

    private static String nameArity(String name, int arity) {
        return name + "/" + arity;
    }

    private static String fieldKey(String qualifiedName) {
        return qualifiedName == null ? "?" : qualifiedName;
    }

    private static boolean isRecordAccessor(CtType<?> t, CtMethod<?> m) {
        if (!(t instanceof CtRecord rec)) return false;
        if (!m.getParameters().isEmpty()) return false;
        return rec.getRecordComponents().stream()
                .anyMatch(rc -> rc.getSimpleName().equals(m.getSimpleName()));
    }

    private boolean isGeneratedType(CtType<?> t) {
        var pos = t.getPosition();
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) return false;
        return generatedFiles.contains(pos.getFile().getAbsolutePath());
    }

    static boolean overridesExternal(CtMethod<?> m) {
        for (CtMethod<?> top : m.getTopDefinitions()) {
            CtType<?> dt = top.getDeclaringType();
            if (dt == null || dt.getPosition() == null || !dt.getPosition().isValidPosition()) {
                return true;
            }
        }
        String n = m.getSimpleName();
        return (n.equals("equals") || n.equals("hashCode") || n.equals("toString"))
                && m.getModifiers().contains(ModifierKind.PUBLIC);
    }

    private static void section(StringBuilder out, String title, List<String> items) {
        line(out, "## " + title + " (" + items.size() + ")");
        for (String s : items) line(out, "  " + s);
        line(out, "");
    }

    private static void line(StringBuilder out, String s) {
        out.append(s).append('\n');
    }
}
