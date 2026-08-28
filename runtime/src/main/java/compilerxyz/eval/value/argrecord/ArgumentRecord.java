package compilerxyz.eval.value.argrecord;

import compilerxyz.gen.BindingSetter;



import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.gen.AspectField;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.util.stream.Collectors;
import compilerxyz.gen.GenAspectField;
import compilerxyz.artifact.Artifact;
import compilerxyz.eval.value.object_.ValueObjectConst;

@BuiltinLight(uniqueId = 6100L)
public record ArgumentRecord(TOrderedMap<ConstKey, ArgumentEntry> fields)
    implements Type, Value, AspectField, ExporterAny {

  @Override
  public String typeDesc() {
    var keys =
        fields.keySet().stream().map(ConstKey::constKeyToCanonical).collect(Collectors.joining(", "));
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
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
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
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.ARGUMENT_RECORD_IDENTITY, ArgumentRecord.class);
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
    var destOpt =
        AspectLookup.getMaybe(xx, destType, GenTrivialIdentityAspects.RECORD_IDENTITY, Record.class);
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
    return EResult.ok(
        new CodeValue(
            cv.code(), EvalOutcome.ofValue(new Record(Opt.none(), convertedFields))));
  }
  @Override
  public EResult<Artifact> artifactableExportArtifact(
      ExprContext xx, QExprId loc) {
    return Value.exportArtifact(xx, loc, this);
  }
}
