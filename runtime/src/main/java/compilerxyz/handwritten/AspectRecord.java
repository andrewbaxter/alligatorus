package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;
import compilerxyz.util.TOrderedMap;

@BuiltinAspect(uniqueId = 1002L, kind = "Record", javadoc = "Identity aspect for record")
public interface AspectRecord extends Aspect {
  TOrderedMap<ConstKey, Type> aspectRecordFieldTypes();

  TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(
      ExprContext xx, QExprId loc, Value value);
}
