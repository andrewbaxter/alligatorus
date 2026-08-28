package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1054L, identityId = 4054L)
public interface AspectOperatorOr extends Aspect {
    EResult<CodeValue> aspectOperatorOrOr(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
