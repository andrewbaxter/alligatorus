package compilerxyz.eval.value.record;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;

public interface RecordBase {

  EResult<TargetBindSub> recordBaseBindSub(ExprContext xx, QExprId loc, Code carry, Type recordType);

  RecordBase recordBaseCloneBase();

  CodeValue recordBaseFieldAccess(
      ExprContext xx, QExprId loc, Code selfCodeRaw, String key, Type entryType);

  EResult<CodeDynamic> recordBaseToDynamic(QExprId loc, Code carry);
}
