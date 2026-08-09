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

@BuiltinLight(uniqueId = 6100L)
public record ArgumentRecord(TOrderedMap<ConstKey, ArgumentEntry> fields)
    implements Type, Value, AspectField, ExporterAny {

  @Override
  public String typeDesc() {
    var keys =
        fields.keySet().stream().map(ConstKey::toCanonical).collect(Collectors.joining(", "));
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
    if (!(key.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "ArgumentRecord::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, some.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    var entry = fields.getOrNull(ck);
    if (entry == null) {
      return EResult.err(loc, "Field '" + ck.toCanonical() + "' not found on argument record");
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    if (!(AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.ARGUMENT_RECORD_IDENTITY, ArgumentRecord.class) instanceof Opt.Some<ArgumentRecord> oSome)) return false;
    var o = oSome.value();
    if (fields.size() != o.fields.size()) return false;
    for (var entry : fields.entrySet()) {
      var otherField = o.fields.getOrNull(entry.getKey());
      if (otherField == null) return false;
      if (!entry.getValue().type().typeIsEqual(xx, otherField.type())) return false;
    }
    return true;
  }

  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    if (!(AspectLookup.getMaybe(xx, destType, GenTrivialIdentityAspects.RECORD_IDENTITY, Record.class)
        instanceof Opt.Some<Record> destSome)) {
      return false;
    }
    var dest = destSome.value();
    if (dest.fields().size() != fields.size()) return false;
    for (var k : dest.fields().keySet()) {
      if (fields.getOrNull(k) == null) return false;
    }
    return true;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    var destOpt =
        AspectLookup.getMaybe(xx, destType, GenTrivialIdentityAspects.RECORD_IDENTITY, Record.class);
    if (!(destOpt instanceof Opt.Some<Record> destSome)) {
      throw new IllegalStateException(
          "ArgumentRecord::typeCastTo: dest is not a record type (typeCanCastTo returned true)");
    }
    var dest = destSome.value();
    var destFieldTypes = dest.aspectRecordFieldTypes();
    var erb = new Erb(loc);
    var convertedFields = new TOrderedMap<ConstKey, RecordField>();
    for (var destEntry : destFieldTypes.entrySet()) {
      var key = destEntry.getKey();
      var srcEntry = fields.getOrNull(key);
      if (srcEntry == null) {
        return EResult.err(
            loc, "ArgumentRecord::typeCastTo: missing field '" + key.toCanonical() + "'");
      }
      var srcCv = srcEntry.binding().bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc));
      var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destEntry.getValue(), srcCv));
      if (convOpt.isEmpty()) return EResult.errs(erb.errors());
      convertedFields.put(key, new RecordField.EvalRes(EResult.ok(convOpt.get())));
    }
    return EResult.ok(
        new CodeValue(
            cv.code(), EvalOutcome.ofValue(new Record(Opt.none(), convertedFields))));
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.artifact.Artifact> exportArtifact(
      compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc) {
    return compilerxyz.gen.Value.super.exportArtifact(xx, loc);
  }
}
