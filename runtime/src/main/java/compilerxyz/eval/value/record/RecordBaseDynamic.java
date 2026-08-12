package compilerxyz.eval.value.record;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.util.Opt;
import compilerxyz.util.TOrderedMap;

public record RecordBaseDynamic(ValueDynamicSource source) {

  public EResult<TargetBindSub> recordBaseBindSub(
      ExprContext xx, QExprId loc, Code carry, Type recordType) {
    return xx.target.targetCodeToBindSub(loc, carry, recordType);
  }

  public EResult<CodeDynamic> recordBaseToDynamic(QExprId loc, Code carry) {
    var dyn = carry.codeDynamic();
    if (dyn == null) {
      return EResult.err(loc, "RecordBaseDynamic::toDynamic: carry has no dynamic code");
    }
    return EResult.ok(dyn);
  }
}
