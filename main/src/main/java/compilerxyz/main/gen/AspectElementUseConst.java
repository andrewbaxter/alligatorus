package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1038L, identityId = 4038L)
public interface AspectElementUseConst extends Aspect {
    EResult<CodeValue> aspectElementUseConstElementUseConst(ExprContext xx, QExprId loc, CodeValue cv);
}
