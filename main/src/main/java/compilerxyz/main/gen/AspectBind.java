package compilerxyz.main.gen;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1022L, identityId = 4022L)
@BuiltinPath("eval.aspect_bind")
public interface AspectBind extends Aspect {
    @BuiltinFull(uniqueId = 1044L)
    @BuiltinPath("types.bind_result")
    record BindResult(Code code, Binding binding) {}

    EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv);
}
