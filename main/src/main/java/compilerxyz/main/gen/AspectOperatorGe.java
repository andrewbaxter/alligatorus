package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1062L, identityId = 4062L)
public interface AspectOperatorGe extends Aspect {
    EResult<CodeValue> aspectOperatorGeGe(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
