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
import spoon.reflect.declaration.CtSealable;
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
      if (t instanceof CtInterface<?> iface) {
        String _cimName = iface.getSimpleName();
        if (!_cimName.isEmpty()) {
          String _cimPrefix = Character.toLowerCase(_cimName.charAt(0)) + _cimName.substring(1);
          for (CtMethod<?> _cimM : iface.getMethods()) {
            if (_cimM.hasModifier(ModifierKind.STATIC) || _cimM.hasModifier(ModifierKind.PRIVATE)) continue;
            if (OBJECT_METHODS.contains(_cimM.getSimpleName())) continue;
            if (!_cimM.getTopDefinitions().isEmpty()) continue;
            if (_cimM.getSimpleName().startsWith(_cimPrefix)) continue;
            report(
                "InterfaceMethodNamePrefix",
                _cimM,
                "Method '" + _cimM.getSimpleName() + "' in interface '" + _cimName
                    + "' should be prefixed with '" + _cimPrefix + "'.");
          }
        }
      }
    }
    for (CtBinaryOperator<?> op : model.getElements(new TypeFilter<>(CtBinaryOperator.class))) {
      if (op.getKind() == BinaryOperatorKind.INSTANCEOF && !op.isImplicit()) {
        CtTypeReference<?> _cisOperandType = op.getLeftHandOperand().getType();
        if (_cisOperandType != null) {
          CtType<?> _cisDecl = _cisOperandType.getTypeDeclaration();
          if (_cisDecl != null) {
            boolean _cisSealed = _cisDecl instanceof CtSealable _s && !_s.getPermittedTypes().isEmpty();
            if (_cisSealed) {
              report(
                  "NoInstanceofOnSealed",
                  op,
                  "'" + _cisOperandType.getSimpleName()
                      + "' is sealed; discriminate it with an exhaustive switch instead of instanceof.");
            }
          }
        }
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
      String _ssTrSnippet;
      {
        var _ssTrPos = tr.getPosition();
        if (_ssTrPos == null || !_ssTrPos.isValidPosition() || _ssTrPos.getCompilationUnit() == null) {
          _ssTrSnippet = "";
        } else {
          String _ssTrSrc = _ssTrPos.getCompilationUnit().getOriginalSourceCode();
          int _ssTrStart = _ssTrPos.getSourceStart();
          int _ssTrEnd = _ssTrPos.getSourceEnd();
          _ssTrSnippet = (_ssTrSrc == null || _ssTrStart < 0 || _ssTrEnd < _ssTrStart
              || _ssTrEnd >= _ssTrSrc.length())
              ? "" : _ssTrSrc.substring(_ssTrStart, _ssTrEnd + 1).trim();
        }
      }
      if (JDK_OPTIONAL.contains(tr.getQualifiedName())
          && tr.getRoleInParent() == CtRole.TYPE
          && _ssTrSnippet.contains("Optional")) {
        report("AvoidJdkOptional", tr, "Use compilerxyz.util.Opt instead of java.util.Optional.");
      }
      {
        var _fqnPos = tr.getPosition();
        if (_fqnPos != null && _fqnPos.isValidPosition() && _fqnPos.getCompilationUnit() != null) {
          if (tr.getParent(spoon.reflect.declaration.CtImport.class) == null) {
            String _fqnFile = _fqnPos.getFile() != null ? _fqnPos.getFile().getName() : "";
            if (!_fqnFile.equals("module-info.java")) {
              String _fqnSrc = _fqnPos.getCompilationUnit().getOriginalSourceCode();
              int _fqnStart = _fqnPos.getSourceStart();
              int _fqnEnd = _fqnPos.getSourceEnd();
              if (_fqnSrc != null && _fqnStart >= 0 && _fqnEnd >= _fqnStart && _fqnEnd < _fqnSrc.length()) {
                String _fqnSnippet = _fqnSrc.substring(_fqnStart, _fqnEnd + 1).trim();
                if (_fqnSnippet.contains(".") && FQN.matcher(_fqnSnippet).matches()) {
                  String _fqnQn = tr.getQualifiedName();
                  if (_fqnQn != null && _fqnSnippet.equals(_fqnQn.replace('$', '.'))) {
                    report(
                        "UnnecessaryFullyQualifiedName",
                        tr,
                        "Import '" + _fqnSnippet + "' and use its simple name (unless ambiguous).");
                  }
                }
              }
            }
          }
        }
      }
    }
    return findings;
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
