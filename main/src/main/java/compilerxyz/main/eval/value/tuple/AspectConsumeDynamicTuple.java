package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.TList;

public record AspectConsumeDynamicTuple(Tuple type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        var types = new TList<Type>(type.elements.size());
        for (var e : type.elements) {
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::jvmClass: self has non-Dynamic element");
            }
            types.add(d.type());
        }
        var sortedTypes = Tuple.sortTupleTypes(xx, types);
        var klass = CompositeClassEmitter.getOrEmit(xx, sortedTypes);
        return JavaDataDescriptor.fromInternalName(
                JavaInternalName.of(klass.getName().replace('.', '/')));
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var carry = cv.code();
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Tuple::toDynamic: CodeValue has no value");
        }
        var someValue = cv.value().optUnwrap();
        if (someValue instanceof ValueDynamic) {
            return DynUtil.dynamicConsumeToDynamic(loc, cv);
        }
        if (!(someValue instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::toDynamic: expected Tuple value, got " + someValue.getClass());
        }
        if (tuple.base.optIsSome()) {
            return DynUtil.wrapWithCarry(
                    loc, carry, tuple.base.optUnwrap().source().valueDynamicSourceRealize(loc));
        }
        var types = new TList<Type>(type.elements.size());
        for (var e : type.elements) {
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::toDynamic: self has non-Dynamic element");
            }
            types.add(d.type());
        }
        if (tuple.elements.size() != types.size()) {
            throw new IllegalStateException("Tuple::toDynamic: arity mismatch between value and type");
        }
        TList<ExprErr> errors = null;
        var dynamicElements = new TList<CodeDynamic>(tuple.elements.size());
        for (int i = 0; i < tuple.elements.size(); i++) {
            var elem = tuple.elements.get(i);
            var elemType = types.get(i);
            TupleElement.EvalRes er;
            switch (elem) {
                case TupleElement.EvalRes r -> er = r;
                case TupleElement.Dynamic d ->
                    throw new IllegalStateException("Tuple::toDynamic: value tuple has Dynamic-only element");
            }
            var elemRes = er.result();
            if (elemRes.hasErrors()) {
                if (errors == null) errors = new TList<>();
                elemRes.drainInto(errors);
                continue;
            }
            var elemCv = elemRes.requireValue("Tuple::toDynamic element value");
            var lookup = GenAspectConsumeDynamic.get(xx, loc, elemType);
            if (lookup.hasErrors()) {
                if (errors == null) errors = new TList<>();
                lookup.drainInto(errors);
                continue;
            }
            var toDyn = lookup.requireValue("Tuple::toDynamic consume aspect");
            var lowered = toDyn.aspectConsumeDynamicToDynamic(xx, loc, elemCv);
            if (lowered.hasErrors()) {
                if (errors == null) errors = new TList<>();
                lowered.drainInto(errors);
                continue;
            }
            var loweredCode = lowered.requireValue("Tuple::toDynamic element lowered");
            dynamicElements.add(loweredCode);
        }
        if (errors != null && !errors.isEmpty()) {
            return EResult.errs(errors);
        }
        var slotOf = Tuple.slotOfIndex(xx, types);
        var sortedTypes = Tuple.sortTupleTypes(xx, types);
        @SuppressWarnings("unchecked")
        CodeDynamic[] sortedCodesArr = new CodeDynamic[types.size()];
        for (int i = 0; i < types.size(); i++) {
            sortedCodesArr[slotOf.get(i)] = dynamicElements.get(i);
        }
        var sortedCodes = new TList<CodeDynamic>(types.size());
        for (var c : sortedCodesArr) sortedCodes.add(c);
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
        var argSeq = new JavaBytecodeSequence();
        for (var c : sortedCodes) argSeq.add(c);
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
        return DynUtil.wrapWithCarry(
                loc, carry, JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, new CodeDynamic(loc, argSeq)));
    }
}
