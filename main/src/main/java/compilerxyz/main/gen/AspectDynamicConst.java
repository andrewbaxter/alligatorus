package compilerxyz.main.gen;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1048L, identityId = 4046L)
@BuiltinPath("eval.aspect_dynamic_const")
public interface AspectDynamicConst extends Aspect {
    Type aspectDynamicConstConstForm(ExprContext xx);
}
