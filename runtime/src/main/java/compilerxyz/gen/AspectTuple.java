package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.TList;

@BuiltinFull(uniqueId = 1013L, identityId = 4013L)
public interface AspectTuple extends Aspect {
    TList<Type> aspectTupleElementTypes();

    TList<EResult<CodeValue>> aspectTupleTakeElements(ExprContext xx, QExprId loc, Value value);
}
