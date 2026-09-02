package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Artifactable;
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
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.stream.Collectors;

@BuiltinFull(uniqueId = 6100L)
public record ArgumentRecord(TOrderedMap<ConstKey, ArgumentEntry> fields) implements Type, Value, AspectField {

    @Override
    public String typeDesc() {
        var keys = fields.keySet().stream().map(ConstKey::constKeyToCanonical).collect(Collectors.joining(", "));
        return "argument_record(" + keys + ")";
    }

    public Type valueType() {
        return this;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.ARGUMENT_RECORD_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "ArgumentRecord::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var entry = fields.getOrNull(ck);
        if (entry == null) {
            return EResult.err(loc, "Field '" + ck.constKeyToCanonical() + "' not found on argument record");
        }
        return EResult.ok(entry.binding().bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc)));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
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
                xx, other, GenTrivialIdentityAspects.ARGUMENT_RECORD_IDENTITY, ArgumentRecord.class);
        if (oOpt.optIsNone()) return false;
        var o = oOpt.optUnwrap();
        if (fields.size() != o.fields.size()) return false;
        for (var entry : fields.entrySet()) {
            var otherField = o.fields.getOrNull(entry.getKey());
            if (otherField == null) return false;
            if (!entry.getValue().type().typeIsEqual(xx, otherField.type())) return false;
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
        if (dest.fields().size() != fields.size()) return false;
        for (var k : dest.fields().keySet()) {
            if (fields.getOrNull(k) == null) return false;
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
        var destFieldTypes = dest.aspectRecordFieldTypes();
        var erb = new Erb(loc);
        var convertedFields = new TOrderedMap<ConstKey, RecordField>();
        for (var destEntry : destFieldTypes.entrySet()) {
            var key = destEntry.getKey();
            var srcEntry = fields.getOrNull(key);
            if (srcEntry == null) {
                return EResult.err(
                        loc, "ArgumentRecord::typeCastTo: missing field '" + key.constKeyToCanonical() + "'");
            }
            var srcCv = srcEntry.binding().bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc));
            var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destEntry.getValue(), srcCv));
            if (convOpt.optIsNone()) return EResult.errs(erb.errors());
            convertedFields.put(key, new RecordField.EvalRes(EResult.ok(convOpt.optUnwrap())));
        }
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(new Record(Opt.none(), convertedFields))));
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }
}
