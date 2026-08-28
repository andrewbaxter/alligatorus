package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1036L, identityId = 4036L)
public interface AspectFieldUseConst extends Aspect {
    EResult<CodeValue> aspectFieldUseConstFieldUseConst(ExprContext xx, QExprId loc, CodeValue cv);
}
