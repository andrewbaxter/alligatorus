package compilerxyz.lint.reach;

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
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ReachabilityMain {

    private final Set<String> generatedFiles;
    private final Set<String> dispatchRoots;

    public ReachabilityMain(Set<String> generatedFiles, Set<String> dispatchRoots) {
        this.generatedFiles = generatedFiles;
        this.dispatchRoots = dispatchRoots;
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

    public int run(CtModel model, ModelIndex index) throws IOException {
        List<CtType<?>> types = new ArrayList<>();
        for (CtType<?> t : model.getAllTypes()) {
            collectTypes(t, types);
        }
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
        Set<String> unmatchedRoots = new HashSet<>(dispatchRoots);
        for (CtType<?> t : types) {
            for (CtExecutable<?> ex : executablesOf(t)) {
                if (!(ex instanceof CtMethod<?> m)) continue;
                String rootKey = t.getQualifiedName() + "#" + m.getSimpleName();
                if (!dispatchRoots.contains(rootKey)) continue;
                unmatchedRoots.remove(rootKey);
                String k = key(ex);
                genReferenced.add(k);
                if (reachable.add(k)) work.add(k);
                onCall(m.getReference(), m.getParameters().size(), work, true);
            }
        }
        if (!unmatchedRoots.isEmpty()) {
            throw new IllegalStateException("dispatch roots not found in the model: " + unmatchedRoots);
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
                    var acc = index.initializerAccessesOf(tm.getDeclaringType());
                    for (CtInvocation<?> inv : acc.invocations) {
                        onCall(inv.getExecutable(), inv.getArguments().size(), work, fromGenerated);
                    }
                    for (CtConstructorCall<?> cc : acc.constructorCalls) {
                        onCall(cc.getExecutable(), cc.getArguments().size(), work, fromGenerated);
                    }
                    scanMethodRefs(acc.executableRefs, work, fromGenerated);
                    if (tm.getDeclaringType() instanceof CtClass<?> liveClass) {
                        for (CtAnonymousExecutable ab : liveClass.getAnonymousExecutables()) {
                            var abAcc = index.accessesOf(ab);
                            for (CtInvocation<?> inv : abAcc.invocations) {
                                onCall(inv.getExecutable(), inv.getArguments().size(), work, fromGenerated);
                            }
                            for (CtConstructorCall<?> cc : abAcc.constructorCalls) {
                                onCall(cc.getExecutable(), cc.getArguments().size(), work, fromGenerated);
                            }
                            scanMethodRefs(abAcc.executableRefs, work, fromGenerated);
                            for (CtFieldRead<?> fr : abAcc.fieldReads) {
                                if (fr.getVariable() != null) {
                                    fieldReads.merge(fieldKey(fr.getVariable().getQualifiedName()), 1, Integer::sum);
                                }
                            }
                            for (CtFieldWrite<?> fw : abAcc.fieldWrites) {
                                if (fw.getVariable() != null) {
                                    fieldWrites.merge(fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                                    if (ModelIndex.isRead(fw))
                                        fieldReads.merge(
                                                fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                                }
                            }
                        }
                    }
                    for (CtFieldRead<?> fr : acc.fieldReads) {
                        if (fr.getVariable() != null) {
                            fieldReads.merge(fieldKey(fr.getVariable().getQualifiedName()), 1, Integer::sum);
                        }
                    }
                    for (CtFieldWrite<?> fw : acc.fieldWrites) {
                        if (fw.getVariable() != null) {
                            fieldWrites.merge(fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                            if (ModelIndex.isRead(fw))
                                fieldReads.merge(fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                        }
                    }
                }
            }
            var wlAcc = index.accessesOf(ex);
            for (CtTypeReference<?> artTr : wlAcc.typeReferences) {
                String artQn = artTr.getQualifiedName();
                if (artQn != null && !artQn.isEmpty()) referencedTypes.add(artQn);
            }
            for (CtInvocation<?> inv : wlAcc.invocations) {
                onCall(inv.getExecutable(), inv.getArguments().size(), work, fromGenerated);
            }
            for (CtConstructorCall<?> cc : wlAcc.constructorCalls) {
                onCall(cc.getExecutable(), cc.getArguments().size(), work, fromGenerated);
            }
            scanMethodRefs(wlAcc.executableRefs, work, fromGenerated);
            for (CtFieldRead<?> fr : wlAcc.fieldReads) {
                if (fr.getVariable() != null) {
                    fieldReads.merge(fieldKey(fr.getVariable().getQualifiedName()), 1, Integer::sum);
                }
            }
            for (CtFieldWrite<?> fw : wlAcc.fieldWrites) {
                if (fw.getVariable() != null) {
                    fieldWrites.merge(fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                    if (ModelIndex.isRead(fw))
                        fieldReads.merge(fieldKey(fw.getVariable().getQualifiedName()), 1, Integer::sum);
                }
            }
        }
        Set<String> liveTypes = new HashSet<>();
        for (CtType<?> t : types) {
            if (executablesOf(t).stream().anyMatch(e -> reachable.contains(key(e)))) {
                liveTypes.add(t.getQualifiedName());
            }
        }
        boolean liveChanged = true;
        while (liveChanged) {
            liveChanged = false;
            for (CtTypeReference<?> tr : index.typeReferences) {
                CtElement enclosing = tr;
                boolean live = false;
                while (enclosing != null && enclosing.isParentInitialized()) {
                    enclosing = enclosing.getParent();
                    if (enclosing instanceof CtType<?> et && liveTypes.contains(et.getQualifiedName())) {
                        live = true;
                        break;
                    }
                }
                if (live) {
                    String qn = tr.getQualifiedName();
                    if (qn != null && !qn.isEmpty()) {
                        referencedTypes.add(qn);
                        if (liveTypes.add(qn)) liveChanged = true;
                    }
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
                boolean iam = false;
                for (CtMethod<?> top : m.getTopDefinitions()) {
                    if (!key(top).equals(key(m)) && top.isAbstract()) {
                        iam = true;
                        break;
                    }
                }
                boolean inlinable = !isRoot(ex)
                        && !rooted
                        && !"compilerxyz.prelude.util.Assert".equals(qn)
                        && m.getBody() != null
                        && !overridesExternal(m)
                        && !iam
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
        section(out, "#1 DEAD PRIVATE FIELDS (0 reads; write-only counts as dead)", deadFields);
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
        Path reportPath = Path.of("lint/target/reachability-report.txt");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, out.toString());
        System.out.println("(report written to " + reportPath + ")");
        return total;
    }

    private void scanMethodRefs(
            List<CtExecutableReferenceExpression<?, ?>> refs, Deque<String> work, boolean fromGenerated) {
        for (CtExecutableReferenceExpression<?, ?> mr : refs) {
            CtExecutableReference<?> r = mr.getExecutable();
            onCall(r, r == null ? 0 : r.getParameters().size(), work, fromGenerated);
        }
    }

    private void onCall(CtExecutableReference<?> ref, int actualArity, Deque<String> work, boolean fromGenerated) {
        if (ref == null) return;
        CtExecutable<?> decl = ref.getExecutableDeclaration();
        String declKey = decl == null ? null : key(decl);
        String counted = null;
        if (declKey != null) {
            counted = declKey;
            if (reachable.add(counted)) work.add(counted);
        }
        CtTypeReference<?> refDecl = ref.getDeclaringType();
        int refArity = ref.getParameters().size();
        Set<Integer> arities = new LinkedHashSet<>();
        arities.add(actualArity);
        if (refArity != actualArity) arities.add(refArity);
        Set<String> matched = new LinkedHashSet<>();
        for (int arity : arities) {
            for (String cand : keysByNameArity.getOrDefault(nameArity(ref.getSimpleName(), arity), List.of())) {
                CtExecutable<?> cex = execByKey.get(cand);
                if (cex == null) continue;
                if (cand.equals(declKey) || subtypeOf(cex, refDecl)) {
                    if (reachable.add(cand)) work.add(cand);
                    matched.add(cand);
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
                if (cand.equals(declKey) || subtypeOf(cex, refDecl)) {
                    if (reachable.add(cand)) work.add(cand);
                    matched.add(cand);
                }
            }
        }
        if (counted == null && matched.size() == 1) {
            counted = matched.iterator().next();
        }
        if (counted != null) {
            callCounts.merge(counted, 1, Integer::sum);
            if (fromGenerated) genReferenced.add(counted);
        }
    }

    private final Map<String, Boolean> subtypeCache = new HashMap<>();

    private boolean subtypeOf(CtExecutable<?> cex, CtTypeReference<?> superRef) {
        if (superRef == null) return false;
        if (!(cex instanceof CtTypeMember tm) || tm.getDeclaringType() == null) return false;
        CtTypeReference<?> declaring = tm.getDeclaringType().getReference();
        if (superRef instanceof CtTypeParameterReference) {
            return declaring.isSubtypeOf(superRef);
        }
        return subtypeCache.computeIfAbsent(
                declaring.getQualifiedName() + "<:" + superRef.toString(), k -> declaring.isSubtypeOf(superRef));
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
