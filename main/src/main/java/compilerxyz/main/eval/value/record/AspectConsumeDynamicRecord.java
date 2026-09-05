package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.TList;

public record AspectConsumeDynamicRecord(Record type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        var slotInfo = Record.computeSlotInfoFromTypes(xx, Record.recordFieldTypesRaw(type.fields));
        var klass = CompositeClassEmitter.getOrEmit(xx, slotInfo.sortedTypes());
        return JavaDataDescriptor.fromInternalName(
                JavaInternalName.of(klass.getName().replace('.', '/')));
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var carry = cv.code();
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Record::toDynamic: CodeValue has no value");
        }
        if (!(cv.value().optUnwrap() instanceof Record record)) {
            throw new IllegalStateException("Record::toDynamic: expected Record value, got "
                    + cv.value().optUnwrap().getClass());
        }
        if (record.base.optIsSome()) {
            var dynCast = CodeDynamic.castOpt(loc, carry);
            if (dynCast.hasErrors()) return dynCast.reerr();
            if (dynCast.requireValue("_rbDyn").optIsNone())
                return EResult.err(loc, "RecordBaseDynamic::toDynamic: carry has no dynamic code");
            var dyn = dynCast.requireValue("_rbDyn").optUnwrap();
            return EResult.ok(dyn);
        }
        TList<ExprErr> errors = null;
        record LoweredField(ConstKey key, String desc, Type type, CodeDynamic code) {}
        var loweredFields = new TList<LoweredField>(record.fields.size());
        for (var entry : record.fields.entrySet()) {
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
            {
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
                var loweredCode = lowered.requireValue("Record::toDynamic field lowered");
                loweredFields.add(new LoweredField(
                        key, toDyn.aspectConsumeDynamicJvmClass(xx).value(), elemType, loweredCode));
            }
        }
        if (errors != null && !errors.isEmpty()) {
            return EResult.errs(errors);
        }
        loweredFields.sort((a, b) -> {
            int c = a.desc().compareTo(b.desc());
            if (c != 0) return c;
            return ConstKey.CANONICAL_ORDER.compare(a.key(), b.key());
        });
        var sortedTypes = new TList<Type>(loweredFields.size());
        var entryCodes = new TList<CodeDynamic>(loweredFields.size());
        for (var f : loweredFields) {
            sortedTypes.add(f.type());
            entryCodes.add(f.code());
        }
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
        var argSeq = new JavaBytecodeSequence();
        for (var c : entryCodes) argSeq.add(c);
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
        return DynUtil.wrapWithCarry(
                loc, carry, JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, new CodeDynamic(loc, argSeq)));
    }
}
