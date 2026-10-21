package compilerxyz.main.gen;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1117L, identityId = 4117L)
@BuiltinPath("eval.aspect_const_dynamic")
public interface AspectConstDynamic extends Aspect {
    Type aspectConstDynamicDynamicForm(ExprContext xx);
}
