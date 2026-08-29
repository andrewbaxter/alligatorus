package compilerxyz.lint.reach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ImmutabilityChecks {

    private final Set<String> mapTypes = Set.of(
            "compilerxyz.prelude.util.TMap",
            "compilerxyz.prelude.util.TOrderedMap",
            "compilerxyz.prelude.util.IMap",
            "java.util.Map",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.TreeMap",
            "java.util.SortedMap",
            "java.util.NavigableMap",
            "java.util.EnumMap",
            "java.util.AbstractMap",
            "java.util.concurrent.ConcurrentHashMap",
            "java.util.concurrent.ConcurrentMap");
    private final Set<String> objectsEquality = Set.of("equals", "deepEquals");

    private final List<ConventionChecks.Finding> findings = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> generatedFiles;
    private final ImmutabilityCheck immutability = new ImmutabilityCheck();

    public ImmutabilityChecks(Set<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<ConventionChecks.Finding> run(ModelIndex index) {
        for (CtType<?> t : index.types) {
            boolean tokenAnnotated = t.getAnnotations().stream()
                    .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("IdentityToken"));
            if (tokenAnnotated) {
                boolean shape = t instanceof CtClass<?>
                        && !(t instanceof CtRecord)
                        && t.hasModifier(ModifierKind.FINAL)
                        && t.getFields().stream().allMatch(f -> f.hasModifier(ModifierKind.STATIC));
                if (!shape) {
                    report(
                            "IdentityTokenValid",
                            t,
                            "'" + t.getQualifiedName() + "' is annotated @IdentityToken but is not a field-less"
                                    + " final class; identity tokens must have no instance state and must not be"
                                    + " records (an empty record makes all instances equal).");
                }
            }
            if (!(t instanceof CtInterface) && ImmutabilityCheck.typeImmutableImplementer(t, new HashSet<>())) {
                var tokenRes = immutability.checkType(t);
                if (!tokenRes.immutable()) {
                    var alt = valueVerify(t, index);
                    if (!alt.immutable()) {
                        report(
                                "ImmutableTypeValid",
                                t,
                                "'" + t.getQualifiedName() + "' implements TypeImmutable but is not immutable:"
                                        + " structural: " + tokenRes.reason() + "; value: " + alt.reason());
                    }
                }
            }
            boolean annotated = t.getAnnotations().stream()
                    .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("Immutable"));
            if (!annotated) continue;
            var res = immutability.checkType(t);
            if (!res.immutable()) {
                report(
                        "ImmutableAnnotationValid",
                        t,
                        "'" + t.getQualifiedName() + "' is annotated @Immutable but is not immutable: " + res.reason());
            }
        }
        for (CtField<?> f : index.fields) {
            boolean enumValue = f instanceof CtEnumValue;
            boolean interfaceConstant = f.getDeclaringType() instanceof CtInterface;
            if (!f.hasModifier(ModifierKind.STATIC) && !enumValue && !interfaceConstant) continue;
            String name = f.getDeclaringType().getQualifiedName() + "." + f.getSimpleName();
            if (!f.hasModifier(ModifierKind.FINAL) && !enumValue && !interfaceConstant) {
                report("StaticFieldImmutable", f, "static field '" + name + "' is not final.");
                continue;
            }
            var res = immutability.checkRef(f.getType(), Map.of());
            if (!res.immutable()) {
                var init = f.getDefaultExpression();
                if (init instanceof CtNewClass<?> nc && nc.getAnonymousClass() != null) {
                    res = immutability.checkExact(nc.getAnonymousClass());
                } else if (init instanceof CtConstructorCall<?> cc) {
                    res = immutability.checkExactRef(cc.getType());
                    if (!res.immutable()) res = immutability.checkValue(init);
                } else if (init != null) {
                    res = immutability.checkValue(init);
                }
            }
            if (!res.immutable()) {
                report(
                        "StaticFieldImmutable",
                        f,
                        "static field '" + name + "' has a non-immutable type: " + res.reason());
            }
        }
        for (CtTypeReference<?> tr : index.typeReferences) {
            if (!mapTypes.contains(tr.getQualifiedName())) continue;
            List<CtTypeReference<?>> args = tr.getActualTypeArguments();
            if (args.size() < 2) continue;
            if (args.get(0).isImplicit()) continue;
            if (tr.getRoleInParent() == CtRole.INTERFACE || tr.getRoleInParent() == CtRole.SUPER_TYPE) {
                continue;
            }
            if (tr.isParentInitialized()
                    && tr.getParent() instanceof CtParameter<?> mirroredParam
                    && mirroredParam.getParent() instanceof CtMethod<?> mirroredMethod
                    && ReachabilityMain.overridesExternal(mirroredMethod)) {
                continue;
            }
            Map<String, ImmutabilityCheck.Res> scope = new HashMap<>();
            CtElement cur = tr.isParentInitialized() ? tr.getParent() : null;
            while (cur != null) {
                if (cur instanceof CtFormalTypeDeclarer d) {
                    for (CtTypeParameter p : d.getFormalCtTypeParameters()) {
                        scope.putIfAbsent(p.getSimpleName(), new ImmutabilityCheck.Res(true, ""));
                    }
                }
                cur = cur.isParentInitialized() ? cur.getParent() : null;
            }
            var res = immutability.checkRef(args.get(0), scope);
            if (!res.immutable()) {
                report("MapKeyImmutable", tr, "map key type '" + args.get(0) + "' is not immutable: " + res.reason());
            }
        }
        for (CtBinaryOperator<?> op : index.binaryOperators) {
            if (op.getKind() != BinaryOperatorKind.EQ && op.getKind() != BinaryOperatorKind.NE) continue;
            CtExpression<?> l = op.getLeftHandOperand();
            CtExpression<?> r = op.getRightHandOperand();
            if (isNullLiteral(l) || isNullLiteral(r)) continue;
            CtTypeReference<?> lt = l.getType();
            CtTypeReference<?> rt = r.getType();
            if (lt == null || rt == null) continue;
            if (lt.isPrimitive() || rt.isPrimitive()) continue;
            if (isEnum(lt) || isEnum(rt)) continue;
            if (isIdentityToken(lt) || isIdentityToken(rt)) continue;
            CtType<?> enclosing = op.getParent(CtType.class);
            while (enclosing != null && enclosing.getParent(CtType.class) != null) {
                enclosing = enclosing.getParent(CtType.class);
            }
            if (enclosing != null && "compilerxyz.prelude.util.Assert".equals(enclosing.getQualifiedName())) {
                continue;
            }
            report(
                    "NoReferenceEquality",
                    op,
                    "'" + (op.getKind() == BinaryOperatorKind.EQ ? "==" : "!=")
                            + "' on references compares object identity; use the type's defined equality"
                            + " (identity aspects for types).");
        }
        for (CtInvocation<?> inv : index.invocations) {
            var ex = inv.getExecutable();
            if (ex == null) continue;
            CtTypeReference<?> declaring = ex.getDeclaringType();
            if (declaring != null
                    && "java.util.Objects".equals(declaring.getQualifiedName())
                    && objectsEquality.contains(ex.getSimpleName())) {
                report(
                        "NoReflectiveEquals",
                        inv,
                        "java.util.Objects." + ex.getSimpleName()
                                + " is a reflective equality check; call the type's own equality directly.");
                continue;
            }
            if (!ex.getSimpleName().equals("equals") || ex.getParameters().size() != 1) continue;
            CtExpression<?> target = inv.getTarget();
            if (target == null) continue;
            CtTypeReference<?> tt = target.getType();
            if (tt == null) continue;
            CtType<?> tdecl = tt.getTypeDeclaration();
            if (tdecl == null) continue;
            if (tdecl instanceof CtRecord || tdecl.isEnum() || tdecl instanceof CtInterface) continue;
            boolean overridden = false;
            CtType<?> equalsHolder = tdecl;
            while (!overridden) {
                for (CtMethod<?> m : equalsHolder.getMethods()) {
                    if (m.getSimpleName().equals("equals") && m.getParameters().size() == 1) {
                        overridden = true;
                    }
                }
                if (overridden) break;
                CtTypeReference<?> sc = equalsHolder.getSuperclass();
                if (sc == null || "java.lang.Object".equals(sc.getQualifiedName())) break;
                CtType<?> next = sc.getTypeDeclaration();
                if (next == null) {
                    overridden = true;
                    break;
                }
                equalsHolder = next;
            }
            if (!overridden) {
                report(
                        "NoReflectiveEquals",
                        inv,
                        "'" + tdecl.getQualifiedName()
                                + "' defines no equals; this resolves to Object's reflective identity equals.");
            }
        }
        return findings;
    }

    public ImmutabilityCheck.Res valueVerify(CtType<?> t, ModelIndex index) {
        if (!(t instanceof CtClass) || !t.hasModifier(ModifierKind.FINAL)) {
            return new ImmutabilityCheck.Res(false, "not a final class");
        }
        for (CtField<?> f : t.getFields()) {
            if (!f.hasModifier(ModifierKind.STATIC) && !f.hasModifier(ModifierKind.FINAL)) {
                return new ImmutabilityCheck.Res(false, "field '" + f.getSimpleName() + "' is not final");
            }
        }
        Set<String> initMethods = new HashSet<>();
        for (CtMethod<?> m : t.getMethods()) {
            for (CtInvocation<?> i : m.getElements(new TypeFilter<>(CtInvocation.class))) {
                if ("setAccessible".equals(i.getExecutable().getSimpleName())) {
                    initMethods.add(m.getSimpleName());
                }
            }
        }
        if (initMethods.isEmpty()) {
            return new ImmutabilityCheck.Res(false, "no reflective two-pass init methods to value-verify");
        }
        String qn = t.getQualifiedName();
        for (CtConstructorCall<?> cc : index.constructorCalls) {
            if (cc.getType() == null || !qn.equals(cc.getType().getQualifiedName())) continue;
            if (cc instanceof CtNewClass) {
                return new ImmutabilityCheck.Res(false, "anonymous subclass construction");
            }
            var ctor = cc.getExecutable() == null ? null : cc.getExecutable().getExecutableDeclaration();
            var args = cc.getArguments();
            for (int i = 0; i < args.size(); i++) {
                var pr = ctor != null && i < ctor.getParameters().size()
                        ? immutability.checkRef(ctor.getParameters().get(i).getType(), Map.of())
                        : new ImmutabilityCheck.Res(false, "unresolvable constructor");
                var r = pr.immutable() ? pr : immutability.checkValue(args.get(i));
                if (!r.immutable()) {
                    return new ImmutabilityCheck.Res(false, "constructor argument " + i + ": " + r.reason());
                }
            }
        }
        for (CtInvocation<?> inv : index.invocations) {
            var ex = inv.getExecutable();
            if (ex == null || !initMethods.contains(ex.getSimpleName())) continue;
            if (ex.getDeclaringType() == null
                    || !qn.equals(ex.getDeclaringType().getQualifiedName())) {
                continue;
            }
            for (CtExpression<?> a : inv.getArguments()) {
                var r = immutability.checkValue(a);
                if (!r.immutable()) {
                    return new ImmutabilityCheck.Res(
                            false, "init argument at " + inv.getPosition() + ": " + r.reason());
                }
            }
        }
        return new ImmutabilityCheck.Res(true, "");
    }

    private static boolean isNullLiteral(CtExpression<?> e) {
        return e instanceof CtLiteral<?> lit && lit.getValue() == null;
    }

    private static boolean isEnum(CtTypeReference<?> ref) {
        CtType<?> decl = ref.getTypeDeclaration();
        return decl != null && decl.isEnum();
    }

    private static boolean isIdentityToken(CtTypeReference<?> ref) {
        CtType<?> decl = ref.getTypeDeclaration();
        return decl != null
                && decl.getAnnotations().stream()
                        .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("IdentityToken"));
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
