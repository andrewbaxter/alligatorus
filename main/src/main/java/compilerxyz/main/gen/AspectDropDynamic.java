package compilerxyz.main.gen;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1032L, identityId = 4032L)
@BuiltinPath("eval.aspect_drop_dynamic")
public interface AspectDropDynamic extends Aspect {
    EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv);
}
