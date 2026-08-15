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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Whole-program dead-code (#1) and single-use (#2) analysis over the source model (Spoon). Returns
 * the total finding count; AnalysisMain adds it to the combined total and fails the build if
 * nonzero. Because this is source-level, generics are not erased, references to compile-time
 * constants are visible, and implemented interfaces are in the model — so classes used only those
 * ways are not false positives. Roots are main methods, JUnit tests, and
 * {@code @Builtin*}/{@code @CodegenDescriptor} members (dynamically dispatched or
 * emitted-into-target entrypoints).
 */
public final class ReachabilityMain {

  private final Set<String> generatedFiles;

  public ReachabilityMain(Set<String> generatedFiles) {
    this.generatedFiles = generatedFiles;
  }

  private static final Set<String> JUNIT =
      Set.of(
          "Test", "RepeatedTest", "TestFactory", "ParameterizedTest",
          "BeforeEach", "AfterEach", "BeforeAll", "AfterAll");
  private static final String CODEGEN_DESCRIPTOR = "CodegenDescriptor";

  private final Map<String, CtExecutable<?>> execByKey = new HashMap<>();
  private final Map<String, List<String>> keysByNameArity = new HashMap<>();
  private final Set<String> reachable = new HashSet<>();
  private final Map<String, Integer> callCounts = new HashMap<>();
  private final Map<String, Integer> fieldReads = new HashMap<>();
  private final Map<String, Integer> fieldWrites = new HashMap<>();
  private final Set<String> referencedTypes = new HashSet<>();

  public int run(CtModel model) {
    // Exclude codegen: it uses external library types (JavaPoet, JavaParser, Palantir) that
    // noClasspath mode cannot resolve, producing systematic false positives in the call graph.
    // Convention checks still run on codegen via ConventionChecks; reachability focuses on runtime.
    // Spoon's getAllTypes() omits inner classes of interfaces; collect them recursively.
    List<CtType<?>> types = new ArrayList<>();
    for (CtType<?> t : model.getAllTypes()) {
      collectTypes(t, types);
    }
    types.removeIf(t -> {
      String qn = t.getQualifiedName();
      return qn == null || qn.startsWith("compilerxyz.codegen.");
    });
    // Index all executables.
    for (CtType<?> t : types) {
      for (CtExecutable<?> ex : executablesOf(t)) {
        String k = key(ex);
        execByKey.put(k, ex);
        keysByNameArity
            .computeIfAbsent(nameArity(ex.getSimpleName(), ex.getParameters().size()), x -> new ArrayList<>())
            .add(k);
      }
    }
    // Seed roots.
    Deque<String> work = new ArrayDeque<>();
    for (CtType<?> t : types) {
      boolean rootedType = isGeneratedType(t) || hasSuppressUnused(t);
      for (CtExecutable<?> ex : executablesOf(t)) {
        if (rootedType || isRoot(ex) || (ex instanceof CtMethod<?> me && me.getBody() != null && overridesExternal(me))) {
          String k = key(ex);
          if (reachable.add(k)) work.add(k);
        }
      }
    }
    // Root methods registered as TypeMethodDynamic targets.
    // TypeMethodDynamic invokes Java methods by string name via ASM bytecode, invisible to CHA.
    // Scan all TypeMethodDynamic constructor calls, extract the methodName string (arg index 4),
    // and root every in-model method with that name so they don't appear as dead or single-use.
    Set<String> dynMethodNames = new HashSet<>();
    for (CtType<?> t : types) {
      for (CtConstructorCall<?> cc : t.getElements(new TypeFilter<>(CtConstructorCall.class))) {
        if (!"TypeMethodDynamic".equals(cc.getType().getSimpleName())) continue;
        List<CtExpression<?>> args = cc.getArguments();
        if (args.size() > 4 && args.get(4) instanceof CtLiteral<?> lit && lit.getValue() instanceof String n) {
          dynMethodNames.add(n);
        }
      }
    }
    // Also root methods called via callStaticMethod/callStaticInterfaceMethod with literal names.
    // These emit direct bytecodes to call static methods by name, invisible to CHA.
    // The method name is at arg index 2: (CodeLoc, JavaInternalName, String methodName, ...).
    for (CtType<?> t : types) {
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
          if (reachable.add(k)) work.add(k);
        }
      }
    }
    // Root methods annotated with @BuiltinPath (called via builtin registration, invisible to CHA).
    for (CtType<?> t : types) {
      for (CtExecutable<?> ex : executablesOf(t)) {
        if (hasAnnotationNamed(ex, "BuiltinPath")) {
          String k = key(ex);
          if (reachable.add(k)) work.add(k);
        }
      }
    }
    // Fixpoint.
    Set<String> fieldInitDone = new HashSet<>();
    while (!work.isEmpty()) {
      String workKey = work.poll();
      CtExecutable<?> ex = execByKey.get(workKey);
      if (ex == null) continue;
      if (ex instanceof CtTypeMember tm && tm.getDeclaringType() != null) {
        String tn = tm.getDeclaringType().getQualifiedName();
        if (fieldInitDone.add(tn)) {
          for (CtField<?> f : tm.getDeclaringType().getFields()) {
            var init = f.getDefaultExpression();
            if (init == null) continue;
            for (CtInvocation<?> inv : init.getElements(new TypeFilter<>(CtInvocation.class))) {
              onCall(inv.getExecutable(), inv.getArguments().size(), work);
            }
            for (CtConstructorCall<?> cc : init.getElements(new TypeFilter<>(CtConstructorCall.class))) {
              onCall(cc.getExecutable(), cc.getArguments().size(), work);
            }
          }
        }
      }
      addReferencedTypesOf(ex);
      for (CtInvocation<?> inv : ex.getElements(new TypeFilter<>(CtInvocation.class))) {
        onCall(inv.getExecutable(), inv.getArguments().size(), work);
      }
      for (CtConstructorCall<?> cc : ex.getElements(new TypeFilter<>(CtConstructorCall.class))) {
        onCall(cc.getExecutable(), cc.getArguments().size(), work);
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
    return report(types);
  }

  private void onCall(CtExecutableReference<?> ref, int actualArity, Deque<String> work) {
    if (ref == null) return;
    CtExecutable<?> decl = ref.getExecutableDeclaration();
    if (decl != null) {
      String dk = key(decl);
      callCounts.merge(dk, 1, Integer::sum);
      // Mark the directly-resolved declaration reachable; this handles generic/varargs calls where
      // the arity-based fallback below may compute the wrong arity from the reference.
      if (reachable.add(dk)) work.add(dk);
    }
    // Reach all same-name/arity overriders that are subtypes of the reference's declaring type
    // (class-hierarchy analysis). Use the actual argument count from the call site since
    // ref.getParameters() may return 0 when noClasspath can't resolve generic/lambda types.
    CtTypeReference<?> refDecl = ref.getDeclaringType();
    int refArity = ref.getParameters().size();
    // Prefer the actual call-site arity; fall back to the reference arity when they differ.
    // Also try varargs methods (1 varargs param handles any call arity).
    Set<Integer> arities = new java.util.LinkedHashSet<>();
    arities.add(actualArity);
    if (refArity != actualArity) arities.add(refArity);
    for (int arity : arities) {
      for (String cand : keysByNameArity.getOrDefault(nameArity(ref.getSimpleName(), arity), List.of())) {
        CtExecutable<?> cex = execByKey.get(cand);
        if (cex == null) continue;
        boolean direct = isDirect(ref, cex);
        boolean sub = !direct && refDecl != null && subtypeOf(cex, refDecl);
        if (refDecl == null || direct || sub) {
          if (reachable.add(cand)) work.add(cand);
        }
      }
    }
    // Also check varargs methods with 1 parameter when the call has != 1 argument.
    if (actualArity != 1) {
      for (String cand : keysByNameArity.getOrDefault(nameArity(ref.getSimpleName(), 1), List.of())) {
        CtExecutable<?> cex = execByKey.get(cand);
        if (cex == null) continue;
        boolean isVarargs =
            !cex.getParameters().isEmpty()
                && cex.getParameters().get(cex.getParameters().size() - 1).isVarArgs();
        if (!isVarargs) continue;
        if (refDecl == null || isDirect(ref, cex) || subtypeOf(cex, refDecl)) {
          if (reachable.add(cand)) work.add(cand);
        }
      }
    }
  }

  private static boolean isDirect(CtExecutableReference<?> ref, CtExecutable<?> cex) {
    CtExecutable<?> decl = ref.getExecutableDeclaration();
    return decl != null && cex == decl;
  }

  private boolean subtypeOf(CtExecutable<?> cex, CtTypeReference<?> superRef) {
    if (!(cex instanceof CtTypeMember tm) || tm.getDeclaringType() == null) return false;
    try {
      CtTypeReference<?> ct = tm.getDeclaringType().getReference();
      if (ct.isSubtypeOf(superRef)) return true;
    } catch (RuntimeException ignored) {
    }
    String sn = superRef.getSimpleName();
    if (sn == null) return false;
    CtType<?> dt = tm.getDeclaringType();
    String dn = dt.getSimpleName();
    if (dn != null && dn.equals(sn)) return true;
    try {
      for (CtTypeReference<?> iface : dt.getSuperInterfaces()) {
        if (sn.equals(iface.getSimpleName())) return true;
      }
      CtTypeReference<?> superClass = dt.getSuperclass();
      if (superClass != null && sn.equals(superClass.getSimpleName())) return true;
    } catch (RuntimeException ignored) {
    }
    return false;
  }

  private void addReferencedTypesOf(CtExecutable<?> ex) {
    for (CtTypeReference<?> tr : ex.getElements(new TypeFilter<>(CtTypeReference.class))) {
      String qn = tr.getQualifiedName();
      if (qn != null && !qn.isEmpty()) referencedTypes.add(qn);
    }
  }

  private boolean isRoot(CtExecutable<?> ex) {
    if (hasAnnotation(ex, JUNIT) || hasAnnotationNamed(ex, CODEGEN_DESCRIPTOR)) return true;
    if (hasSuppressUnusedAnnotation(ex)) return true;
    return ex instanceof CtMethod<?> m
        && m.getSimpleName().equals("main")
        && m.getModifiers().contains(ModifierKind.STATIC)
        && m.getParameters().size() == 1;
  }

  private static boolean hasSuppressUnusedAnnotation(CtExecutable<?> ex) {
    return ex.getAnnotations().stream()
        .filter(a -> "SuppressWarnings".equals(a.getAnnotationType().getSimpleName()))
        .anyMatch(a -> {
          var v = a.getValues().get("value");
          return v != null && v.toString().contains("unused");
        });
  }

  // ---- reporting ----

  private int report(List<CtType<?>> types) {
    // A live type (has a reachable method or is a builtin root) keeps every type it names —
    // superclass, implemented (marker) interfaces, field types — alive as a referenced type.
    for (CtType<?> t : types) {
      boolean live =
          executablesOf(t).stream().anyMatch(e -> reachable.contains(key(e)))
              ;
      if (live) {
        for (CtTypeReference<?> tr : t.getElements(new TypeFilter<>(CtTypeReference.class))) {
          String qn = tr.getQualifiedName();
          if (qn != null && !qn.isEmpty()) referencedTypes.add(qn);
        }
      }
    }

    List<String> deadClasses = new ArrayList<>();
    List<String> deadMethods = new ArrayList<>();
    List<String> singleUseMethods = new ArrayList<>();
    List<String> singleUseFields = new ArrayList<>();

    for (CtType<?> t : types) {
      String qn = t.getQualifiedName();
      List<CtExecutable<?>> execs = executablesOf(t);
      boolean anyReachable = execs.stream().anyMatch(e -> reachable.contains(key(e)));
      boolean rooted = isGeneratedType(t) || hasSuppressUnused(t);
      boolean isAnnotation = t.isAnnotationType();
      if (!anyReachable && !rooted && !isAnnotation && !referencedTypes.contains(qn)) {
        deadClasses.add(qn);
        continue;
      }
      for (CtExecutable<?> ex : execs) {
        if (!(ex instanceof CtMethod<?> m)) continue;
        String k = key(ex);
        boolean concrete = m.getBody() != null;
        boolean inlinable =
            !isRoot(ex)
                && !rooted
                && m.getBody() != null
                && !overridesExternal(m)
                && !implementsAbstractMethod(m)
                && !isRecordAccessor(t, m);
        if (!reachable.contains(k)
            && concrete
            && !rooted
            && !isRoot(ex)
            && !m.getSimpleName().equals("main")
            && !overridesExternal(m)
            && !isRecordAccessor(t, m)) {
          deadMethods.add(qn + "#" + sig(ex));
        }
        if (inlinable && reachable.contains(k) && callCounts.getOrDefault(k, 0) == 1) {
          singleUseMethods.add(qn + "#" + sig(ex));
        }
      }
      boolean recordOrEnum = t.isEnum() || (t instanceof spoon.reflect.declaration.CtRecord);
      if (!recordOrEnum) {
        for (CtField<?> f : t.getFields()) {
          if (f.getModifiers().contains(ModifierKind.PRIVATE)
              && !f.getModifiers().contains(ModifierKind.FINAL)
              && !f.getModifiers().contains(ModifierKind.VOLATILE)
              && fieldWrites.getOrDefault(fieldKey(f.getReference().getQualifiedName()), 0) == 0
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
    section(out, "#2 SINGLE-USE PRIVATE FIELDS (1 read)", singleUseFields);
    int total = deadClasses.size() + deadMethods.size() + singleUseMethods.size() + singleUseFields.size();
    line(
        out,
        String.format(
            "TOT:  dead classes=%d  dead methods=%d  single-use methods=%d  single-use fields=%d",
            deadClasses.size(), deadMethods.size(), singleUseMethods.size(), singleUseFields.size()));

    System.out.print(out);
    try {
      Path report = Path.of("analysis/target/reachability-report.txt");
      Files.createDirectories(report.getParent());
      Files.writeString(report, out.toString());
      System.out.println("(report written to " + report + ")");
    } catch (IOException e) {
      // best-effort
    }
    return total;
  }

  private boolean isGeneratedType(CtType<?> t) {
    var pos = t.getPosition();
    if (pos == null || !pos.isValidPosition() || pos.getFile() == null) return false;
    return generatedFiles.contains(pos.getFile().getAbsolutePath());
  }

  private static boolean implementsAbstractMethod(CtMethod<?> m) {
    try {
      for (CtMethod<?> top : m.getTopDefinitions()) {
        if (top != m && top.isAbstract()) return true;
      }
    } catch (RuntimeException ignored) {
      return false;
    }
    return false;
  }

  private boolean overridesExternal(CtMethod<?> m) {
    try {
      for (CtMethod<?> top : m.getTopDefinitions()) {
        CtType<?> dt = top.getDeclaringType();
        if (dt == null || dt.getPosition() == null || !dt.getPosition().isValidPosition()) {
          return true; // declared in a type not in our source model (JDK/library)
        }
      }
    } catch (RuntimeException e) {
      return true;
    }
    // Also treat overrides of Object methods as external.
    String n = m.getSimpleName();
    return (n.equals("equals") || n.equals("hashCode") || n.equals("toString"))
        && m.getModifiers().contains(ModifierKind.PUBLIC);
  }

  // ---- helpers ----

  private static void collectTypes(CtType<?> t, List<CtType<?>> out) {
    out.add(t);
    for (CtType<?> nested : t.getNestedTypes()) {
      collectTypes(nested, out);
    }
    // Spoon's getNestedTypes() omits anonymous classes from field initializers
    // (e.g. `static final Iface DELEGATE = new Iface() {...}`). Include them so
    // the BFS can resolve interface dispatch to their implementations.
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
    if (t instanceof spoon.reflect.declaration.CtClass<?> c) {
      for (CtConstructor<?> ctor : c.getConstructors()) out.add(ctor);
    }
    return out;
  }

  private static String key(CtExecutable<?> ex) {
    String owner =
        ex instanceof CtTypeMember tm && tm.getDeclaringType() != null
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

  private static boolean hasAnnotation(CtExecutable<?> ex, Set<String> simpleNames) {
    return ex.getAnnotations().stream()
        .anyMatch(a -> simpleNames.contains(a.getAnnotationType().getSimpleName()));
  }

  private static boolean hasAnnotationNamed(CtExecutable<?> ex, String simpleName) {
    return ex.getAnnotations().stream()
        .anyMatch(a -> a.getAnnotationType().getSimpleName().equals(simpleName));
  }

  private static boolean isRecordAccessor(CtType<?> t, CtMethod<?> m) {
    if (!(t instanceof spoon.reflect.declaration.CtRecord rec)) return false;
    if (!m.getParameters().isEmpty()) return false;
    return rec.getRecordComponents().stream()
        .anyMatch(rc -> rc.getSimpleName().equals(m.getSimpleName()));
  }

  private static boolean hasSuppressUnused(CtType<?> t) {
    return t.getAnnotations().stream()
        .filter(a -> "SuppressWarnings".equals(a.getAnnotationType().getSimpleName()))
        .anyMatch(a -> {
          var v = a.getValues().get("value");
          return v != null && v.toString().contains("unused");
        });
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
