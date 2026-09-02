package compilerxyz.lint.reach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtSealable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ImmutabilityCheck {

    public record Res(boolean immutable, String reason) {}

    private static final Res OK = new Res(true, "");

    private final Set<String> leaves = Set.of(
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Character",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Void",
            "java.math.BigInteger",
            "java.math.BigDecimal",
            "java.util.regex.Pattern",
            "java.lang.Class",
            "java.net.URI",
            "java.nio.file.Path",
            "com.squareup.javapoet.ClassName",
            "com.squareup.javapoet.TypeName",
            "com.squareup.javapoet.ParameterizedTypeName",
            "com.squareup.javapoet.ArrayTypeName",
            "com.squareup.javapoet.TypeVariableName",
            "com.squareup.javapoet.WildcardTypeName",
            "com.squareup.javapoet.CodeBlock",
            "com.squareup.javapoet.AnnotationSpec",
            "com.squareup.javapoet.FieldSpec",
            "com.squareup.javapoet.MethodSpec",
            "com.squareup.javapoet.ParameterSpec",
            "com.squareup.javapoet.TypeSpec");
    private final Set<String> containers = Set.of("compilerxyz.prelude.util.IList", "compilerxyz.prelude.util.IMap");

    private final Map<String, Res> memo = new HashMap<>();
    private final Set<String> inProgress = new HashSet<>();
    private final Set<String> assumed = new HashSet<>();

    public Res checkType(CtType<?> decl) {
        if (containers.contains(decl.getQualifiedName())) return OK;
        Map<String, Res> subst = new HashMap<>();
        for (CtTypeParameter p : decl.getFormalCtTypeParameters()) subst.put(p.getSimpleName(), OK);
        return checkDecl(decl, subst, false);
    }

    public Res checkExact(CtType<?> decl) {
        return checkDecl(decl, new HashMap<>(), true);
    }

    public Res checkExactRef(CtTypeReference<?> ref) {
        CtType<?> decl = ref == null ? null : ref.getTypeDeclaration();
        if (decl == null) return checkRef(ref, new HashMap<>());
        return checkDecl(decl, argSubst(decl, ref.getActualTypeArguments(), new HashMap<>()), true);
    }

    public Res checkRef(CtTypeReference<?> ref, Map<String, Res> subst) {
        if (ref == null) return new Res(false, "unresolvable type");
        if (ref instanceof CtWildcardReference w) {
            CtTypeReference<?> bound = w.getBoundingType();
            if (bound == null || !w.isUpper()) return new Res(false, "wildcard without an upper bound");
            return checkRef(bound, subst);
        }
        if (ref instanceof CtTypeParameterReference) {
            Res r = subst.get(ref.getSimpleName());
            if (r == null) return new Res(false, "unbound type parameter '" + ref.getSimpleName() + "'");
            return r;
        }
        if (ref.isPrimitive()) return OK;
        if (ref instanceof CtArrayTypeReference) return new Res(false, "arrays are mutable");
        String qn = ref.getQualifiedName();
        if (qn == null) return new Res(false, "unresolvable type");
        if (leaves.contains(qn)) return OK;
        if ("compilerxyz.main.artifact.ArtifactSupport$ArtifactIdHolder".equals(qn)) return OK;
        if (containers.contains(qn)) {
            List<CtTypeReference<?>> args = ref.getActualTypeArguments();
            if (args.isEmpty()) return new Res(false, "raw '" + ref.getSimpleName() + "'");
            for (CtTypeReference<?> a : args) {
                Res r = checkRef(a, subst);
                if (!r.immutable()) {
                    return new Res(false, "'" + ref.getSimpleName() + "' type argument: " + r.reason());
                }
            }
            return OK;
        }
        CtType<?> decl = ref.getTypeDeclaration();
        if (decl == null) {
            return new Res(false, "external type '" + qn + "' is not on the immutable leaf whitelist");
        }
        if (typeImmutableImplementer(decl, new HashSet<>())) return OK;
        return checkDecl(decl, argSubst(decl, ref.getActualTypeArguments(), subst), false);
    }

    static boolean typeImmutableImplementer(CtType<?> t, Set<String> seen) {
        if (!seen.add(t.getQualifiedName())) return false;
        for (CtTypeReference<?> si : t.getSuperInterfaces()) {
            if ("compilerxyz.main.eval.TypeImmutable".equals(si.getQualifiedName())) return true;
            CtType<?> sd = si.getTypeDeclaration();
            if (sd != null && typeImmutableImplementer(sd, seen)) return true;
        }
        CtTypeReference<?> sc = t.getSuperclass();
        if (sc != null) {
            CtType<?> sd = sc.getTypeDeclaration();
            if (sd != null && typeImmutableImplementer(sd, seen)) return true;
        }
        return false;
    }

    public Res checkValue(CtExpression<?> e) {
        return checkValue(e, new HashMap<>(), 0);
    }

    private Res checkValue(CtExpression<?> e, Map<String, Res> env, int depth) {
        if (depth > 32) return new Res(false, "value analysis depth limit");
        if (e == null) return new Res(false, "unresolvable value");
        if (e instanceof CtLiteral) return OK;
        if (e instanceof CtNewClass) return new Res(false, "anonymous class value");
        if (e instanceof CtConstructorCall<?> cc) {
            CtType<?> decl = cc.getType() == null ? null : cc.getType().getTypeDeclaration();
            if (decl == null) return new Res(false, "unresolvable constructor");
            if (typeImmutableImplementer(decl, new HashSet<>())) return OK;
            var args = cc.getArguments();
            if (decl instanceof CtRecord rec) {
                var components = new ArrayList<>(rec.getRecordComponents());
                if (components.size() != args.size()) {
                    return new Res(false, "record constructor arity mismatch");
                }
                for (int i = 0; i < args.size(); i++) {
                    Res r = checkRef(components.get(i).getType(), new HashMap<>());
                    if (!r.immutable()) r = checkValue(args.get(i), env, depth + 1);
                    if (!r.immutable()) {
                        return new Res(false, "component '" + components.get(i).getSimpleName() + "': " + r.reason());
                    }
                }
                return OK;
            }
            CtExecutable<?> ctor =
                    cc.getExecutable() == null ? null : cc.getExecutable().getExecutableDeclaration();
            if (ctor == null) return new Res(false, "unresolvable constructor");
            var params = ctor.getParameters();
            Map<String, Res> subst = new HashMap<>();
            for (int i = 0; i < Math.min(params.size(), args.size()); i++) {
                if (params.get(i).getType() instanceof CtTypeParameterReference tp
                        && !(params.get(i).getType() instanceof CtWildcardReference)) {
                    Res r = checkValue(args.get(i), env, depth + 1);
                    subst.merge(tp.getSimpleName(), r, (a, b) -> a.immutable() ? b : a);
                }
            }
            return checkDecl(decl, subst, true);
        }
        if (e instanceof CtInvocation<?> inv) {
            var ex = inv.getExecutable();
            String name = ex == null ? "" : ex.getSimpleName();
            String owner = ex == null || ex.getDeclaringType() == null
                    ? ""
                    : ex.getDeclaringType().getQualifiedName();
            if (("of".equals(name) || "from".equals(name) || "ofEntries".equals(name))
                    && (containers.contains(owner) || "compilerxyz.prelude.util.TList".equals(owner))) {
                for (CtExpression<?> a : inv.getArguments()) {
                    Res r = checkValue(a, env, depth + 1);
                    if (!r.immutable()) return new Res(false, "element: " + r.reason());
                }
                return OK;
            }
            if ("compilerxyz.prelude.util.Opt".equals(owner)) {
                if ("none".equals(name)) return OK;
                if ("some".equals(name) && inv.getArguments().size() == 1) {
                    return checkValue(inv.getArguments().get(0), env, depth + 1);
                }
            }
            Res declaredRes = checkRef(inv.getType(), new HashMap<>());
            if (declaredRes.immutable()) return declaredRes;
            if (ex != null
                    && ex.getExecutableDeclaration() instanceof CtExecutable<?> md
                    && md.getBody() != null
                    && md.getBody().getStatements().size() == 1
                    && md.getBody().getStatements().get(0) instanceof CtReturn<?> rtn
                    && rtn.getReturnedExpression() != null
                    && md.getParameters().size() == inv.getArguments().size()) {
                Map<String, Res> callEnv = new HashMap<>();
                for (int i = 0; i < md.getParameters().size(); i++) {
                    callEnv.put(
                            md.getParameters().get(i).getSimpleName(),
                            checkValue(inv.getArguments().get(i), env, depth + 1));
                }
                return checkValue(rtn.getReturnedExpression(), callEnv, depth + 1);
            }
            return declaredRes;
        }
        if (e instanceof CtFieldRead<?> fr
                && fr.getVariable() != null
                && fr.getVariable().getFieldDeclaration() instanceof CtField<?> ff
                && ff.hasModifier(ModifierKind.FINAL)) {
            Res declared = checkRef(ff.getType(), new HashMap<>());
            if (declared.immutable()) return declared;
            if (ff.getDefaultExpression() != null) {
                return checkValue(ff.getDefaultExpression(), new HashMap<>(), depth + 1);
            }
            return declared;
        }
        if (e instanceof CtVariableRead<?> vrp
                && vrp.getVariable() != null
                && vrp.getVariable().getDeclaration() instanceof CtParameter<?> prm
                && env.containsKey(prm.getSimpleName())) {
            return env.get(prm.getSimpleName());
        }
        if (e instanceof CtVariableRead<?> vr
                && vr.getVariable() != null
                && vr.getVariable().getDeclaration() instanceof CtLocalVariable<?> lv) {
            Res traced = traceAccumulator(lv, vr);
            if (traced != null) return traced;
            if (lv.getDefaultExpression() != null) {
                CtElement lvBody = lv.getParent(CtExecutable.class);
                boolean written = false;
                if (lvBody != null) {
                    for (CtVariableWrite<?> w : lvBody.getElements(new TypeFilter<>(CtVariableWrite.class))) {
                        if (w.getVariable() != null
                                && w.getVariable().getSimpleName().equals(lv.getSimpleName())) {
                            written = true;
                            break;
                        }
                    }
                }
                if (!written) {
                    return checkValue(lv.getDefaultExpression(), env, depth + 1);
                }
            }
        }
        return checkRef(e.getType(), new HashMap<>());
    }

    Res traceAccumulator(CtLocalVariable<?> lv, CtVariableRead<?> consumption) {
        var init = lv.getDefaultExpression();
        boolean emptyTList = init instanceof CtConstructorCall<?> icc
                && icc.getType() != null
                && "compilerxyz.prelude.util.TList".equals(icc.getType().getQualifiedName())
                && icc.getArguments().isEmpty();
        if (!emptyTList) return null;
        CtElement body = lv.getParent(CtExecutable.class);
        if (body == null) body = lv.getParent(CtAnonymousExecutable.class);
        if (body == null) return null;
        int consumptionStart = consumption.getPosition().getSourceStart();
        for (CtVariableRead<?> use : body.getElements(new TypeFilter<>(CtVariableRead.class))) {
            if (use.getVariable() == null || !use.getVariable().getSimpleName().equals(lv.getSimpleName())) {
                continue;
            }
            int useStart = use.getPosition().getSourceStart();
            if (useStart == consumptionStart) continue;
            boolean isAddTarget = use.getParent() instanceof CtInvocation<?> pinv
                    && pinv.getTarget() != null
                    && pinv.getTarget().getPosition().isValidPosition()
                    && pinv.getTarget().getPosition().getSourceStart() == useStart
                    && ("add".equals(pinv.getExecutable().getSimpleName())
                            || "addAll".equals(pinv.getExecutable().getSimpleName()));
            if (!isAddTarget) {
                return new Res(false, "accumulator '" + lv.getSimpleName() + "' escapes beyond add/addAll");
            }
            for (CtExpression<?> a : ((CtInvocation<?>) use.getParent()).getArguments()) {
                Res r = checkValue(a);
                if (!r.immutable()) return new Res(false, "accumulated element: " + r.reason());
            }
        }
        return OK;
    }

    private Map<String, Res> argSubst(CtType<?> decl, List<CtTypeReference<?>> args, Map<String, Res> subst) {
        Map<String, Res> out = new HashMap<>();
        List<CtTypeParameter> params = decl.getFormalCtTypeParameters();
        for (int i = 0; i < params.size(); i++) {
            String name = params.get(i).getSimpleName();
            Res r = i < args.size()
                    ? checkRef(args.get(i), subst)
                    : new Res(false, "raw type argument for '" + name + "'");
            out.put(name, r);
        }
        return out;
    }

    private Res checkDecl(CtType<?> decl, Map<String, Res> subst, boolean asSupertype) {
        StringBuilder keyB = new StringBuilder(asSupertype ? "S|" : "F|").append(decl.getQualifiedName());
        for (CtTypeParameter p : decl.getFormalCtTypeParameters()) {
            Res r = subst.get(p.getSimpleName());
            keyB.append('|').append(r != null && r.immutable() ? 'I' : 'M');
        }
        String key = keyB.toString();
        Res cached = memo.get(key);
        if (cached != null) return cached;
        if (inProgress.contains(key)) {
            assumed.add(key);
            return OK;
        }
        inProgress.add(key);
        Res res = OK;
        String qn = decl.getQualifiedName();
        boolean sealed = decl instanceof CtSealable s && !s.getPermittedTypes().isEmpty();
        boolean record = decl instanceof CtRecord;
        if (decl instanceof CtInterface<?>) {
            if ("compilerxyz.main.eval.TypeImmutable".equals(qn)) {
                res = OK;
            } else {
                res = sealed ? permittedRes(decl, subst) : new Res(false, "'" + qn + "' is an open interface");
            }
        } else {
            if (!asSupertype && !record && !decl.isEnum() && !decl.hasModifier(ModifierKind.FINAL) && !sealed) {
                res = new Res(false, "'" + qn + "' is a non-final, non-sealed class");
            }
            if (res.immutable() && !asSupertype && sealed) res = permittedRes(decl, subst);
            if (res.immutable()) {
                for (CtField<?> f : decl.getFields()) {
                    if (f instanceof CtEnumValue) continue;
                    if (f.hasModifier(ModifierKind.STATIC)) continue;
                    if (!record && !f.hasModifier(ModifierKind.FINAL)) {
                        res = new Res(false, "field '" + qn + "." + f.getSimpleName() + "' is not final");
                        break;
                    }
                    Res r = checkRef(f.getType(), subst);
                    if (!r.immutable()) {
                        res = new Res(false, "field '" + qn + "." + f.getSimpleName() + "': " + r.reason());
                        break;
                    }
                }
            }
            if (res.immutable()) {
                CtTypeReference<?> sc = decl.getSuperclass();
                if (sc != null
                        && !"java.lang.Object".equals(sc.getQualifiedName())
                        && !"java.lang.Enum".equals(sc.getQualifiedName())
                        && !"java.lang.Record".equals(sc.getQualifiedName())) {
                    CtType<?> parent = sc.getTypeDeclaration();
                    Res r = parent == null
                            ? new Res(false, "external superclass '" + sc.getQualifiedName() + "'")
                            : checkDecl(parent, argSubst(parent, sc.getActualTypeArguments(), subst), true);
                    if (!r.immutable()) {
                        res = new Res(false, "superclass '" + sc.getSimpleName() + "' of '" + qn + "': " + r.reason());
                    }
                }
            }
        }
        inProgress.remove(key);
        assumed.remove(key);
        if (!res.immutable() || assumed.isEmpty()) memo.put(key, res);
        return res;
    }

    private Res permittedRes(CtType<?> parent, Map<String, Res> subst) {
        List<CtTypeParameter> parentParams = parent.getFormalCtTypeParameters();
        for (CtTypeReference<?> permittedRef : ((CtSealable) parent).getPermittedTypes()) {
            CtType<?> permDecl = permittedRef.getTypeDeclaration();
            if (permDecl == null) {
                return new Res(false, "unresolvable permitted type '" + permittedRef.getQualifiedName() + "'");
            }
            if (permDecl.getQualifiedName().equals(parent.getQualifiedName())) continue;
            CtTypeReference<?> superRef = null;
            List<CtTypeReference<?>> supers = new ArrayList<>(permDecl.getSuperInterfaces());
            if (permDecl.getSuperclass() != null) supers.add(permDecl.getSuperclass());
            for (CtTypeReference<?> s : supers) {
                if (parent.getQualifiedName().equals(s.getQualifiedName())) superRef = s;
            }
            Map<String, Res> permSubst = new HashMap<>();
            for (CtTypeParameter fp : permDecl.getFormalCtTypeParameters()) {
                Res mapped = new Res(
                        false,
                        "type parameter '" + fp.getSimpleName() + "' of '" + permDecl.getQualifiedName()
                                + "' is not constrained by the sealed supertype");
                if (superRef != null) {
                    List<CtTypeReference<?>> superArgs = superRef.getActualTypeArguments();
                    for (int i = 0; i < superArgs.size() && i < parentParams.size(); i++) {
                        if (superArgs.get(i) instanceof CtTypeParameterReference tpr
                                && !(superArgs.get(i) instanceof CtWildcardReference)
                                && tpr.getSimpleName().equals(fp.getSimpleName())) {
                            Res fromParent = subst.get(parentParams.get(i).getSimpleName());
                            if (fromParent != null) mapped = fromParent;
                        }
                    }
                }
                permSubst.put(fp.getSimpleName(), mapped);
            }
            Res r = checkDecl(permDecl, permSubst, false);
            if (!r.immutable()) {
                return new Res(false, "permitted type '" + permDecl.getQualifiedName() + "': " + r.reason());
            }
        }
        return OK;
    }
}
