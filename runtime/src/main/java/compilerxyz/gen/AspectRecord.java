package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;
import compilerxyz.util.TOrderedMap;

@Builtin(uniqueId = 1002L)
@compilerxyz.annotations.BuiltinTypeId(7230L)
public interface AspectRecord extends Aspect {
  TOrderedMap<ConstKey, Type> aspectRecordFieldTypes();

  TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(
      ExprContext xx, QExprId loc, Value value);
}
