package compilerxyz.main.gen;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1117L, identityId = 4117L)
public interface AspectConstDynamic extends Aspect {
    Type aspectConstDynamicDynamicForm(ExprContext xx);
}
