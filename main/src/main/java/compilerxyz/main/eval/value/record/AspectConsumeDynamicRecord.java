package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
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
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectConsumeDynamicRecord(Record type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        var slotInfo = Record.computeSlotInfoFromTypes(xx, Record.slotFieldTypes(type.fields()));
        var klass = CompositeClassEmitter.getOrEmit(xx, slotInfo.sortedTypes());
        return JavaDataDescriptor.fromInternalName(
                JavaInternalName.of(klass.getName().replace('.', '/')));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Record::toDynamic: CodeValue has no value");
        }
        if (cv.value().optUnwrap() instanceof ValueDynamic) {
            return EResult.ok(cv);
        }
        if (!(cv.value().optUnwrap() instanceof Record record)) {
            throw new IllegalStateException("Record::toDynamic: expected Record value, got "
                    + cv.value().optUnwrap().getClass());
        }
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        TList<ExprErr> errors = null;
        record LoweredField(ConstKey key, String desc, Type type, JavaBytecodeReg reg) {}
        var loweredFields = new TList<LoweredField>(record.fields().size());
        for (var entry : record.fields()) {
            var key = entry.getKey();
            var field = entry.getValue();
            var elemRes =
                    switch (field) {
                        case RecordField.Dynamic d ->
                            EResult.<CodeValue>err(
                                    loc,
                                    "Record entry '"
                                            + key.constKeyToCanonical()
                                            + "' is dynamic-only, cannot consume directly to dynamic");
                        case RecordField.EvalRes er -> er.result();
                    };
            if (elemRes.hasErrors()) {
                if (errors == null) errors = new TList<>();
                elemRes.drainInto(errors);
                continue;
            }
            var elemCv = elemRes.requireValue("Record::toDynamic field value");
            if (elemCv.value().optIsNone()) {
                throw new IllegalStateException(
                        "Record::toDynamic: field '" + key.constKeyToCanonical() + "' CodeValue has no value");
            }
            var elemType = elemCv.value().optUnwrap().valueType();
            var toDynOpt = GenAspectConsumeDynamic.getMaybe(xx, elemType);
            if (toDynOpt.optIsNone()) {
                if (errors == null) errors = new TList<>();
                errors.add(new ExprErr(
                        loc,
                        TList.of(new ExprSubErr.Opaque("Record entry '"
                                + key.constKeyToCanonical()
                                + "' type ("
                                + elemType.typeDesc()
                                + ") is missing aspect AspectConsumeDynamic"))));
                continue;
            }
            var toDyn = toDynOpt.optUnwrap();
            var lowered = toDyn.aspectConsumeDynamicToDynamic(xx, loc, elemCv);
            if (lowered.hasErrors()) {
                if (errors == null) errors = new TList<>();
                lowered.drainInto(errors);
                continue;
            }
            var loweredReg = DynUtil.valueReg(loc, lowered.requireValue("Record::toDynamic field lowered"));
            if (loweredReg.hasErrors()) {
                if (errors == null) errors = new TList<>();
                loweredReg.drainInto(errors);
                continue;
            }
            loweredFields.add(new LoweredField(
                    key,
                    toDyn.aspectConsumeDynamicJvmClass(xx).value(),
                    elemType,
                    b.reg(loweredReg.requireValue("Record::toDynamic field reg"))));
        }
        if (errors != null && !errors.isEmpty()) {
            return EResult.errs(errors);
        }
        loweredFields.sort((a, c) -> {
            int cmp = a.desc().compareTo(c.desc());
            if (cmp != 0) return cmp;
            return ConstKey.CANONICAL_ORDER.compare(a.key(), c.key());
        });
        var sortedTypes = new TList<Type>(loweredFields.size());
        var sortedRegs = new TList<JavaBytecodeReg>(loweredFields.size());
        for (var f : loweredFields) {
            sortedTypes.add(f.type());
            sortedRegs.add(f.reg());
        }
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
        var obj = b.newInstance(klass, ctorDesc, sortedRegs);
        var dynamicFields = new TOrderedMap<ConstKey, RecordField>();
        for (var entry : type.fields()) {
            dynamicFields.put(entry.getKey(), new RecordField.Dynamic(Record.recordFieldType(entry.getValue())));
        }
        Value result = new ValueDynamic(new ValueDynamicSource.Reg(obj), new Record(IOrderedMap.from(dynamicFields)));
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.ofValue(result)));
    }
}
