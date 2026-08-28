package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1017L, identityId = 4017L)
@BuiltinPath("eval.aspect_set")
public interface AspectSet extends Aspect {
    EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue self_, CodeValue value);
}
