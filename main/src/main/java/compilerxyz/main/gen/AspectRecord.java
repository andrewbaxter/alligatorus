package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.TOrderedMap;

@BuiltinFull(uniqueId = 1002L, identityId = 4002L)
@BuiltinPath("eval.aspect_record")
public interface AspectRecord extends Aspect {
    TOrderedMap<ConstKey, Type> aspectRecordFieldTypes();

    TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(ExprContext xx, QExprId loc, Value value);
}
