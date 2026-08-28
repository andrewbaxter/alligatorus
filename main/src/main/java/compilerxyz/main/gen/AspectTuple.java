package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 1013L, identityId = 4013L)
public interface AspectTuple extends Aspect {
    TList<Type> aspectTupleElementTypes();

    TList<EResult<CodeValue>> aspectTupleTakeElements(ExprContext xx, QExprId loc, Value value);
}
