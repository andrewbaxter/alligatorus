package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1020L, identityId = 4020L)
public interface AspectOperatorLt extends Aspect {
    EResult<CodeValue> aspectOperatorLtLt(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
