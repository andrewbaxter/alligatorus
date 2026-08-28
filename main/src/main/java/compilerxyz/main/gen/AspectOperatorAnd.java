package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1053L, identityId = 4053L)
public interface AspectOperatorAnd extends Aspect {
    EResult<CodeValue> aspectOperatorAndAnd(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
