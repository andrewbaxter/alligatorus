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
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
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
 * Whole-program dead-code (#1) and single-use (#2) analysis over the source model (Spoon). Because
 * this is source-level, generics are not erased, references to compile-time constants are visible,
 * and implemented interfaces are in the model — so classes used only those ways are not false
 * positives. Roots are main methods, JUnit tests, and {@code @Builtin*}/{@code @CodegenDescriptor}
 * members (dynamically dispatched or emitted-into-target entrypoints).
 */
public final class ReachabilityMain {

  private static final Set<String> JUNIT =
      Set.of(
          "Test", "RepeatedTest", "TestFactory", "ParameterizedTest",
          "BeforeEach", "AfterEach", "BeforeAll", "AfterAll");
  private static final Set<String> BUILTIN =
      Set.of("BuiltinPath", "BuiltinFull", "BuiltinLight", "BuiltinPushStruct");
  private static final String CODEGEN_DESCRIPTOR = "CodegenDescriptor";

  private final Map<String, CtExecutable<?>> execByKey = new HashMap<>();
  private final Map<String, List<String>> keysByNameArity = new HashMap<>();
  private final Set<String> reachable = new HashSet<>();
  private final Map<String, Integer> callCounts = new HashMap<>();
  private final Map<String, Integer> fieldReads = new HashMap<>();
  private final Set<String> referencedTypes = new HashSet<>();

  public void run(CtModel model) {
    List<CtType<?>> types = new ArrayList<>(model.getAllTypes());
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
      boolean rootedType = hasAnyAnnotation(t, BUILTIN);
      for (CtExecutable<?> ex : executablesOf(t)) {
        if (rootedType || isRoot(ex)) {
          String k = key(ex);
          if (reachable.add(k)) work.add(k);
        }
      }
    }
    // Fixpoint.
    while (!work.isEmpty()) {
      CtExecutable<?> ex = execByKey.get(work.poll());
      if (ex == null) continue;
      addReferencedTypesOf(ex);
      for (CtInvocation<?> inv : ex.getElements(new TypeFilter<>(CtInvocation.class))) {
        onCall(inv.getExecutable(), work);
      }
      for (CtConstructorCall<?> cc : ex.getElements(new TypeFilter<>(CtConstructorCall.class))) {
        onCall(cc.getExecutable(), work);
      }
      for (CtFieldRead<?> fr : ex.getElements(new TypeFilter<>(CtFieldRead.class))) {
        if (fr.getVariable() != null) {
          fieldReads.merge(fieldKey(fr.getVariable().getQualifiedName()), 1, Integer::sum);
        }
      }
    }
    report(types);
  }

  private void onCall(CtExecutableReference<?> ref, Deque<String> work) {
    if (ref == null) return;
    CtExecutable<?> decl = ref.getExecutableDeclaration();
    if (decl != null) {
      String dk = key(decl);
      callCounts.merge(dk, 1, Integer::sum);
    }
    // Reach the static target plus all same-name/arity overriders that are subtypes of the
    // reference's declaring type (class-hierarchy analysis).
    CtTypeReference<?> refDecl = ref.getDeclaringType();
    for (String cand : keysByNameArity.getOrDefault(nameArity(ref.getSimpleName(), ref.getParameters().size()), List.of())) {
      CtExecutable<?> cex = execByKey.get(cand);
      if (cex == null) continue;
      if (refDecl == null || isDirect(ref, cex) || subtypeOf(cex, refDecl)) {
        if (reachable.add(cand)) work.add(cand);
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
      return ct.isSubtypeOf(superRef);
    } catch (RuntimeException e) {
      return false;
    }
  }

  private void addReferencedTypesOf(CtExecutable<?> ex) {
    for (CtTypeReference<?> tr : ex.getElements(new TypeFilter<>(CtTypeReference.class))) {
      String qn = tr.getQualifiedName();
      if (qn != null && !qn.isEmpty()) referencedTypes.add(qn);
    }
  }

  private boolean isRoot(CtExecutable<?> ex) {
    if (hasAnnotation(ex, JUNIT) || hasAnnotationNamed(ex, CODEGEN_DESCRIPTOR)) return true;
    return ex instanceof CtMethod<?> m
        && m.getSimpleName().equals("main")
        && m.getModifiers().contains(ModifierKind.STATIC)
        && m.getParameters().size() == 1;
  }

  // ---- reporting ----

  private void report(List<CtType<?>> types) {
    // A live type (has a reachable method or is a builtin root) keeps every type it names —
    // superclass, implemented (marker) interfaces, field types — alive as a referenced type.
    for (CtType<?> t : types) {
      boolean live =
          executablesOf(t).stream().anyMatch(e -> reachable.contains(key(e)))
              || hasAnyAnnotation(t, BUILTIN);
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
      boolean rooted = hasAnyAnnotation(t, BUILTIN);
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
            (m.getModifiers().contains(ModifierKind.PRIVATE)
                    || m.getModifiers().contains(ModifierKind.STATIC))
                && !isRoot(ex)
                && !rooted;
        if (!reachable.contains(k)
            && concrete
            && !rooted
            && !isRoot(ex)
            && !m.getSimpleName().equals("main")
            && !overridesExternal(m)) {
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

  private static boolean hasAnyAnnotation(CtType<?> t, Set<String> simpleNames) {
    return t.getAnnotations().stream()
        .anyMatch(a -> simpleNames.contains(a.getAnnotationType().getSimpleName()));
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
