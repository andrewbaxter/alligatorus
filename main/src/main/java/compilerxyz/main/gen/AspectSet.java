package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1017L, identityId = 4017L)
@BuiltinPath("eval.aspect_set")
public interface AspectSet extends Aspect {
    EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue self_, CodeValue value);
}
