package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1042L, identityId = 4042L)
@BuiltinPath("eval.operator_mod")
public interface AspectOperatorMod extends Aspect {
    EResult<CodeValue> aspectOperatorModMod(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference);
}
