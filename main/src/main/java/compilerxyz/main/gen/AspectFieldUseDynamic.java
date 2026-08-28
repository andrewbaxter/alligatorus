package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1037L, identityId = 4037L)
@BuiltinPath("eval.aspect_field_use_dynamic")
public interface AspectFieldUseDynamic extends Aspect {
    EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(ExprContext xx, QExprId loc, ValueDynamicSource source);
}
