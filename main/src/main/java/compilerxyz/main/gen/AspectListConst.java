package compilerxyz.main.gen;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 1047L, identityId = 4045L)
public interface AspectListConst extends Aspect {
    EResult<TList<Value>> aspectListConstElements(ExprContext xx, QExprId loc, Value value);

    Type aspectListConstElementType(Value value);
}
