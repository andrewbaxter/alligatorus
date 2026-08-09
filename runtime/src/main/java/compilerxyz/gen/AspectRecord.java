package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;
import compilerxyz.util.TOrderedMap;

@BuiltinFull(uniqueId = 1002L, identityId = 4002L)
public interface AspectRecord extends Aspect {
  TOrderedMap<ConstKey, Type> aspectRecordFieldTypes();

  TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(
      ExprContext xx, QExprId loc, Value value);
}
