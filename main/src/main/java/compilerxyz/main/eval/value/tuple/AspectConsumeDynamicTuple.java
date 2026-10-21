package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public record AspectConsumeDynamicTuple(Tuple type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        var types = new TList<Type>(type.elements().size());
        for (var e : type.elements()) {
            switch (e) {
                case TupleElement.Dynamic d -> types.add(d.type());
                case TupleElement.EvalRes held -> {}
            }
        }
        var sortedTypes = Tuple.sortTupleTypes(xx, types);
        var klass = CompositeClassEmitter.getOrEmit(xx, sortedTypes);
        return JavaDataDescriptor.fromInternalName(
                JavaInternalName.of(klass.getName().replace('.', '/')));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Tuple::toDynamic: CodeValue has no value");
        }
        var someValue = cv.value().optUnwrap();
        if (someValue instanceof ValueDynamic) {
            return EResult.ok(cv);
        }
        if (!(someValue instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::toDynamic: expected Tuple value, got " + someValue.getClass());
        }
        var types = new TList<Type>(type.elements().size());
        for (var e : type.elements()) {
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::toDynamic: self has non-Dynamic element");
            }
            types.add(d.type());
        }
        if (tuple.elements().size() != types.size()) {
            throw new IllegalStateException("Tuple::toDynamic: arity mismatch between value and type");
        }
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var errors = new TList<ExprErr>();
        var elementRegs = new TList<JavaBytecodeReg>(tuple.elements().size());
        for (int i = 0; i < tuple.elements().size(); i++) {
            var elem = tuple.elements().get(i);
            var elemType = types.get(i);
            TupleElement.EvalRes er;
            switch (elem) {
                case TupleElement.EvalRes r -> er = r;
                case TupleElement.Dynamic d ->
                    throw new IllegalStateException("Tuple::toDynamic: value tuple has Dynamic-only element");
            }
            var elemRes = er.result();
            if (elemRes.hasErrors()) {
                elemRes.drainInto(errors);
                continue;
            }
            var elemCv = elemRes.requireValue("Tuple::toDynamic element value");
            var lookup = GenAspectConsumeDynamic.get(xx, loc, elemType);
            if (lookup.hasErrors()) {
                lookup.drainInto(errors);
                continue;
            }
            var lowered = lookup.requireValue("Tuple::toDynamic consume aspect")
                    .aspectConsumeDynamicToDynamic(xx, loc, elemCv);
            if (lowered.hasErrors()) {
                lowered.drainInto(errors);
                continue;
            }
            var loweredReg = DynUtil.valueReg(xx, loc, lowered.requireValue("Tuple::toDynamic element lowered"));
            if (loweredReg.hasErrors()) {
                loweredReg.drainInto(errors);
                continue;
            }
            elementRegs.add(b.reg(loweredReg.requireValue("Tuple::toDynamic element reg")));
        }
        if (!errors.isEmpty()) {
            return EResult.errs(errors);
        }
        var slotOf = Tuple.slotOfIndex(xx, types);
        var sortedTypes = Tuple.sortTupleTypes(xx, types);
        var regBySlot = new TMap<Integer, JavaBytecodeReg>();
        for (int i = 0; i < types.size(); i++) {
            regBySlot.put(slotOf.get(i), elementRegs.get(i));
        }
        var sortedRegs = new TList<JavaBytecodeReg>(types.size());
        for (int i = 0; i < types.size(); i++)
            sortedRegs.add(regBySlot.getOpt(i).optUnwrap());
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
        var obj = b.newInstance(klass, ctorDesc, sortedRegs);
        Value result = new ValueDynamic(new ValueDynamicSource.Reg(obj), new Tuple(type.elements()));
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.ofValue(result)));
    }
}
