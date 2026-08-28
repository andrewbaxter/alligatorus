package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1056L, identityId = 4056L)
public interface AspectOperatorShl extends Aspect {
    EResult<CodeValue> aspectOperatorShlShl(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
