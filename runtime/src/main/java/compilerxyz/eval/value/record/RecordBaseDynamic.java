package compilerxyz.eval.value.record;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TOrderedMap;

public final class RecordBaseDynamic implements RecordBase {
  private final ValueDynamic.Source source;

  public RecordBaseDynamic(ValueDynamic.Source source) {
    this.source = source;
  }

  public ValueDynamic.Source source() {
    return source;
  }

  @Override
  public EResult<TargetBindSub> recordBaseBindSub(
      ExprContext xx, QExprId loc, Code carry, Type recordType) {
    return xx.target.targetCodeToBindSub(loc, carry, recordType);
  }

  @Override
  public RecordBase recordBaseCloneBase() {
    return new RecordBaseDynamic(source);
  }

  @Override
  public CodeValue recordBaseFieldAccess(
      ExprContext xx, QExprId loc, String key, Type entryType) {
    var childSource = new ValueDynamic.Source.Field(source, key);
    var fieldCode = new Code(loc, DynUtil.renderSource(loc, childSource));
    if (entryType instanceof Record recordType) {
      var newFields = new TOrderedMap<ConstKey, RecordField>();
      for (var entry : recordType.fields().entrySet()) {
        var t =
            switch (entry.getValue()) {
              case RecordField.Dynamic d -> d.type();
              case RecordField.EvalRes er ->
                  throw new IllegalStateException(
                      "RecordBaseDynamic::fieldAccess: field dynamic type Record has non-Mortar"
                          + " field");
            };
        newFields.put(entry.getKey(), new RecordField.Dynamic(t));
      }
      Value innerRecord = new Record(Opt.some(new RecordBaseDynamic(childSource)), newFields);
      return new CodeValue(fieldCode, Opt.some(innerRecord));
    }
    Value valueDynamic = new ValueDynamic(childSource, entryType);
    return new CodeValue(fieldCode, Opt.some(valueDynamic));
  }

  @Override
  public EResult<CodeDynamic> recordBaseToDynamic(QExprId loc, Code carry) {
    var dyn = carry.dynamic();
    if (dyn == null) {
      return EResult.err(loc, "RecordBaseDynamic::toDynamic: carry has no dynamic code");
    }
    return EResult.ok(dyn);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof RecordBaseDynamic other && source.equals(other.source);
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }
}
