package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1024L, identityId = 4024L)
public interface AspectConsumeDynamic extends Aspect {
    EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv);

    JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx);
}
