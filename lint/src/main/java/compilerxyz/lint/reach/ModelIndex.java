package compilerxyz.lint.reach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ModelIndex {

    public static final class ExecAccesses {
        public final List<CtInvocation<?>> invocations = new ArrayList<>();
        public final List<CtConstructorCall<?>> constructorCalls = new ArrayList<>();
        public final List<CtExecutableReferenceExpression<?, ?>> executableRefs = new ArrayList<>();
        public final List<CtFieldRead<?>> fieldReads = new ArrayList<>();
        public final List<CtFieldWrite<?>> fieldWrites = new ArrayList<>();
        public final List<CtTypeReference<?>> typeReferences = new ArrayList<>();
        public final List<CtVariableAccess<?>> variableAccesses = new ArrayList<>();
    }

    public final List<CtType<?>> types = new ArrayList<>();
    public final List<CtClass<?>> classes = new ArrayList<>();
    public final List<CtInterface<?>> interfaces = new ArrayList<>();
    public final List<CtField<?>> fields = new ArrayList<>();
    public final List<CtTypeReference<?>> typeReferences = new ArrayList<>();
    public final List<CtBinaryOperator<?>> binaryOperators = new ArrayList<>();
    public final List<CtInvocation<?>> invocations = new ArrayList<>();
    public final List<CtConstructorCall<?>> constructorCalls = new ArrayList<>();
    public final List<CtExecutableReferenceExpression<?, ?>> executableRefExpressions = new ArrayList<>();
    public final List<CtAssignment<?, ?>> assignments = new ArrayList<>();
    public final List<CtFieldRead<?>> fieldReads = new ArrayList<>();
    public final List<CtLocalVariable<?>> localVariables = new ArrayList<>();
    public final List<CtMethod<?>> methods = new ArrayList<>();
    public final List<CtConstructor<?>> constructors = new ArrayList<>();
    public final List<CtLambda<?>> lambdas = new ArrayList<>();

    private final Map<String, ExecAccesses> byExecutable = new HashMap<>();
    private final Map<String, ExecAccesses> byTypeInitializers = new HashMap<>();

    public ModelIndex(CtModel model) {
        for (CtElement e : model.getElements(new TypeFilter<>(CtElement.class))) {
            if (e instanceof CtType<?> t) {
                types.add(t);
                if (t instanceof CtClass<?> c) classes.add(c);
                if (t instanceof CtInterface<?> i) interfaces.add(i);
            }
            if (e instanceof CtField<?> f) fields.add(f);
            if (e instanceof CtBinaryOperator<?> b) binaryOperators.add(b);
            if (e instanceof CtAssignment<?, ?> a) assignments.add(a);
            if (e instanceof CtLocalVariable<?> lv) localVariables.add(lv);
            if (e instanceof CtMethod<?> m) methods.add(m);
            if (e instanceof CtConstructor<?> c) constructors.add(c);
            if (e instanceof CtLambda<?> l) lambdas.add(l);
            if (e instanceof CtTypeReference<?> tr) {
                typeReferences.add(tr);
                for (var acc : ownerAccesses(e)) acc.typeReferences.add(tr);
            }
            if (e instanceof CtInvocation<?> inv) {
                invocations.add(inv);
                for (var acc : ownerAccesses(e)) acc.invocations.add(inv);
            }
            if (e instanceof CtConstructorCall<?> cc) {
                constructorCalls.add(cc);
                for (var acc : ownerAccesses(e)) acc.constructorCalls.add(cc);
            }
            if (e instanceof CtExecutableReferenceExpression<?, ?> er) {
                executableRefExpressions.add(er);
                for (var acc : ownerAccesses(e)) acc.executableRefs.add(er);
            }
            if (e instanceof CtVariableAccess<?> va) {
                for (var acc : ownerAccesses(e)) {
                    acc.variableAccesses.add(va);
                    if (e instanceof CtFieldRead<?> fr) acc.fieldReads.add(fr);
                    if (e instanceof CtFieldWrite<?> fw) acc.fieldWrites.add(fw);
                }
                if (e instanceof CtFieldRead<?> fr) fieldReads.add(fr);
            }
        }
    }

    public ExecAccesses accessesOf(CtElement executable) {
        return byExecutable.computeIfAbsent(execKey(executable), k -> new ExecAccesses());
    }

    public ExecAccesses initializerAccessesOf(CtType<?> type) {
        return byTypeInitializers.computeIfAbsent(execKey(type), k -> new ExecAccesses());
    }

    private List<ExecAccesses> ownerAccesses(CtElement e) {
        var out = new ArrayList<ExecAccesses>();
        CtElement owner = e;
        while (owner != null && owner.isParentInitialized()) {
            owner = owner.getParent();
            if (owner instanceof CtMethod || owner instanceof CtConstructor || owner instanceof CtAnonymousExecutable) {
                out.add(byExecutable.computeIfAbsent(execKey(owner), k -> new ExecAccesses()));
            }
            if (owner instanceof CtField<?> field && field.getDeclaringType() != null) {
                out.add(byTypeInitializers.computeIfAbsent(execKey(field.getDeclaringType()), k -> new ExecAccesses()));
            }
        }
        return out;
    }

    private static String execKey(CtElement executable) {
        var pos = executable.getPosition();
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) {
            return "id:" + System.identityHashCode(executable);
        }
        return pos.getFile().getPath() + "#" + pos.getSourceStart();
    }
}
