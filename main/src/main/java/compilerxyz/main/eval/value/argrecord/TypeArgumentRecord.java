package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.stream.Collectors;

@BuiltinFull(uniqueId = 6100L)
public record TypeArgumentRecord(TOrderedMap<ConstKey, Type> fieldTypes) implements Aspect, Type {
    public static TypeArgumentRecord of(TOrderedMap<ConstKey, ArgumentEntry> fields) {
        var types = new TOrderedMap<ConstKey, Type>();
        for (var entry : fields.entrySet()) {
            types.put(entry.getKey(), entry.getValue().type());
        }
        return new TypeArgumentRecord(types);
    }

    @Override
    public String typeDesc() {
        var keys =
                fieldTypes.keySet().stream().map(ConstKey::constKeyToCanonical).collect(Collectors.joining(", "));
        return "argument_record(" + keys + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_ARGUMENT_RECORD_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(AspectFieldArgumentRecord.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_ARGUMENT_RECORD_IDENTITY, TypeArgumentRecord.class);
        if (oOpt.optIsNone()) return false;
        var o = oOpt.optUnwrap();
        if (fieldTypes.size() != o.fieldTypes.size()) return false;
        for (var entry : fieldTypes.entrySet()) {
            var otherTypeOpt = o.fieldTypes.getOpt(entry.getKey());
            if (otherTypeOpt.optIsNone()) return false;
            var otherType = otherTypeOpt.optUnwrap();
            if (!entry.getValue().typeIsEqual(xx, otherType)) return false;
        }
        return true;
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        var destOptCheck = AspectLookup.getMaybe(xx, destType, GenTrivialIdentityAspects.RECORD_IDENTITY, Record.class);
        if (destOptCheck.optIsNone()) {
            return false;
        }
        var dest = destOptCheck.optUnwrap();
        if (dest.fields().size() != fieldTypes.size()) return false;
        for (var k : dest.fields().keys()) {
            if (fieldTypes.getOpt(k).optIsNone()) return false;
        }
        return true;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var destOpt = AspectLookup.getMaybe(xx, destType, GenTrivialIdentityAspects.RECORD_IDENTITY, Record.class);
        if (destOpt.optIsNone()) {
            throw new IllegalStateException(
                    "ArgumentRecord::typeCastTo: dest is not a record type (typeCanCastTo returned true)");
        }
        var dest = destOpt.optUnwrap();
        var destFieldTypes = GenAspectRecord.getMaybe(xx, dest).optUnwrap().aspectRecordFieldTypes();
        var record = MustCast.mustCast(ArgumentRecord.class, cv.value().optUnwrap());
        var erb = new Erb(loc);
        var convertedFields = new TOrderedMap<ConstKey, RecordField>();
        for (var destEntry : destFieldTypes.entrySet()) {
            var key = destEntry.getKey();
            var srcEntryOpt = record.fields().getOpt(key);
            if (srcEntryOpt.optIsNone()) {
                return EResult.err(
                        loc, "ArgumentRecord::typeCastTo: missing field '" + key.constKeyToCanonical() + "'");
            }
            var srcEntry = srcEntryOpt.optUnwrap();
            var srcCv = srcEntry.binding()
                    .bindingUse(
                            xx,
                            loc,
                            new BindingSetterArgumentField(record.fields(), key, record.setter()),
                            Code.none(loc));
            var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destEntry.getValue(), srcCv));
            if (convOpt.optIsNone()) return EResult.errs(erb.errors());
            convertedFields.put(key, new RecordField.EvalRes(EResult.ok(convOpt.optUnwrap())));
        }
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(new Record(IOrderedMap.from(convertedFields)))));
    }

    @Override
    public EResult<ArtifactExporter> typeExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
