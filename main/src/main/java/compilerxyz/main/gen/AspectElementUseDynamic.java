package compilerxyz.main.gen;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1039L, identityId = 4039L)
@BuiltinPath("eval.aspect_element_use_dynamic")
public interface AspectElementUseDynamic extends Aspect {
    EResult<CodeValue> aspectElementUseDynamicElementUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource elementSource, Code carry);
}
