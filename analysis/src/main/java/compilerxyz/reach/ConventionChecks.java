package compilerxyz.reach;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import spoon.reflect.path.CtRole;
import spoon.reflect.CtModel;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * The CLAUDE.md conventions, as source-level Spoon checks (previously Error Prone BugCheckers):
 * interface-method prefix, no instanceof on sealed types, no {@code @Nullable}, no
 * {@code java.util.Optional}, and no unnecessary fully-qualified names. Generated code
 * ({@code compilerxyz.gen.*}) is skipped.
 */
public final class ConventionChecks {

  public record Finding(String check, String file, int line, String message) {}

  private static final Set<String> OBJECT_METHODS = Set.of("equals", "hashCode", "toString");
  private static final Set<String> JDK_OPTIONAL =
      Set.of(
          "java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong",
          "java.util.OptionalDouble");
  // A fully-qualified reference: a lowercase package head, then one or more .Uppercase type segments.
  private static final Pattern FQN = Pattern.compile("[a-z][\\w]*(\\.[a-z][\\w]*)*(\\.[A-Z]\\w*)+");

  private final List<Finding> findings = new ArrayList<>();
  private final Set<String> seen = new HashSet<>();
  private final Set<String> generatedFiles;

  public ConventionChecks(Set<String> generatedFiles) {
    this.generatedFiles = generatedFiles;
  }

  public List<Finding> run(CtModel model) {
    for (CtType<?> t : model.getElements(new TypeFilter<>(CtType.class))) {
      if (t instanceof CtInterface<?> iface) checkInterfaceMethodNames(iface);
    }
    for (CtBinaryOperator<?> op : model.getElements(new TypeFilter<>(CtBinaryOperator.class))) {
      if (op.getKind() == BinaryOperatorKind.INSTANCEOF && !op.isImplicit()) {
        checkInstanceofOnSealed(op);
      }
    }
    for (CtAnnotation<?> a : model.getElements(new TypeFilter<>(CtAnnotation.class))) {
      if (!a.isImplicit()
          && a.getAnnotationType() != null
          && "Nullable".equals(a.getAnnotationType().getSimpleName())) {
        report("NoNullableType", a, "Do not use @Nullable; model optional values with Opt<T>.");
      }
    }
    for (CtTypeReference<?> tr : model.getElements(new TypeFilter<>(CtTypeReference.class))) {
      if (tr.isImplicit()) continue;
      // Flag Optional only where it is explicitly written (a declared type), not var-inferred, so
      // findings point at the API surface (method returns/fields) rather than every propagated local.
      if (JDK_OPTIONAL.contains(tr.getQualifiedName())
          && tr.getRoleInParent() == CtRole.TYPE
          && sourceSnippet(tr).contains("Optional")) {
        report("AvoidJdkOptional", tr, "Use compilerxyz.util.Opt instead of java.util.Optional.");
      }
      checkUnnecessaryFqn(tr);
    }
    return findings;
  }

  private static String sourceSnippet(CtElement e) {
    var pos = e.getPosition();
    if (pos == null || !pos.isValidPosition() || pos.getCompilationUnit() == null) return "";
    String src = pos.getCompilationUnit().getOriginalSourceCode();
    int start = pos.getSourceStart();
    int end = pos.getSourceEnd();
    if (src == null || start < 0 || end < start || end >= src.length()) return "";
    return src.substring(start, end + 1).trim();
  }

  private void checkInterfaceMethodNames(CtInterface<?> iface) {
    String name = iface.getSimpleName();
    if (name.isEmpty()) return;
    String prefix = Character.toLowerCase(name.charAt(0)) + name.substring(1);
    for (CtMethod<?> m : iface.getMethods()) {
      if (m.hasModifier(ModifierKind.STATIC) || m.hasModifier(ModifierKind.PRIVATE)) continue;
      if (OBJECT_METHODS.contains(m.getSimpleName())) continue;
      if (!m.getTopDefinitions().isEmpty()) continue; // overrides a super-interface method
      if (m.getSimpleName().startsWith(prefix)) continue;
      report(
          "InterfaceMethodNamePrefix",
          m,
          "Method '" + m.getSimpleName() + "' in interface '" + name + "' should be prefixed with '"
              + prefix + "'.");
    }
  }

  private void checkInstanceofOnSealed(CtBinaryOperator<?> op) {
    CtTypeReference<?> operandType = op.getLeftHandOperand().getType();
    if (operandType == null) return;
    CtType<?> decl = operandType.getTypeDeclaration();
    if (decl == null) return;
    boolean sealed =
        decl instanceof spoon.reflect.declaration.CtSealable s && !s.getPermittedTypes().isEmpty();
    if (sealed) {
      report(
          "NoInstanceofOnSealed",
          op,
          "'" + operandType.getSimpleName()
              + "' is sealed; discriminate it with an exhaustive switch instead of instanceof.");
    }
  }

  private void checkUnnecessaryFqn(CtTypeReference<?> tr) {
    var pos = tr.getPosition();
    if (pos == null || !pos.isValidPosition() || pos.getCompilationUnit() == null) return;
    if (tr.getParent(spoon.reflect.declaration.CtImport.class) != null) return;
    String file = pos.getFile() != null ? pos.getFile().getName() : "";
    if (file.equals("module-info.java")) return;
    String src = pos.getCompilationUnit().getOriginalSourceCode();
    int start = pos.getSourceStart();
    int end = pos.getSourceEnd();
    if (src == null || start < 0 || end < start || end >= src.length()) return;
    String snippet = src.substring(start, end + 1).trim();
    if (!snippet.contains(".") || !FQN.matcher(snippet).matches()) return;
    // Only flag when the source actually wrote the fully-qualified name (snippet equals the type's
    // qualified name), not merely a member access that happens to look dotted.
    String qn = tr.getQualifiedName();
    if (qn == null || !snippet.equals(qn.replace('$', '.'))) return;
    report(
        "UnnecessaryFullyQualifiedName",
        tr,
        "Import '" + snippet + "' and use its simple name (unless ambiguous).");
  }

  private void report(String check, CtElement e, String message) {
    var pos = e.getPosition();
    if (pos == null || !pos.isValidPosition() || pos.getFile() == null) return;
    String abs = pos.getFile().toPath().toAbsolutePath().normalize().toString();
    if (generatedFiles.contains(abs)) return;
    String file = pos.getFile().getPath();
    if (!seen.add(check + "|" + file + "|" + pos.getLine() + "|" + pos.getColumn())) return;
    findings.add(new Finding(check, file, pos.getLine(), message));
  }
}
