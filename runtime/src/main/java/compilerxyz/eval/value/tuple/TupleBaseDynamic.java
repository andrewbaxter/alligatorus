package compilerxyz.eval.value.tuple;

import compilerxyz.eval.Code;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;

public record TupleBaseDynamic(ValueDynamic.Source source) {

  public EResult<TargetBindSub> tupleBaseBindSub(
      ExprContext xx, QExprId loc, Code carry, Type tupleType) {
    return xx.target.targetCodeToBindSub(loc, carry, tupleType);
  }

  public EResult<CodeDynamic> tupleBaseToDynamic(QExprId loc, Code carry) {
    var dyn = carry.dynamic();
    if (dyn == null) {
      return EResult.err(loc, "TupleBaseDynamic::toDynamic: carry has no dynamic code");
    }
    return EResult.ok(dyn);
  }
}
