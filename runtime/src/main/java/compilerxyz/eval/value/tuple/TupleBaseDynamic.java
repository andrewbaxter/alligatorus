package compilerxyz.eval.value.tuple;

import compilerxyz.eval.Code;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;

public record TupleBaseDynamic(ValueDynamicSource source) {

  public EResult<TargetBindSub> tupleBaseBindSub(
      ExprContext xx, QExprId loc, Code carry, Type tupleType) {
    return xx.target.targetCodeToBindSub(loc, carry, tupleType);
  }

  public EResult<CodeDynamic> tupleBaseToDynamic(QExprId loc, Code carry) {
    var dyn = carry.codeDynamic();
    if (dyn == null) {
      return EResult.err(loc, "TupleBaseDynamic::toDynamic: carry has no dynamic code");
    }
    return EResult.ok(dyn);
  }
}
