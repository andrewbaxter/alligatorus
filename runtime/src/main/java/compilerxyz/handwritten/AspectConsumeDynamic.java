package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;

@BuiltinAspect(
    uniqueId = 1024L,
    kind = "ConsumeDynamic",
    javadoc = "Used to ensure a value is fully dynamic.")
public interface AspectConsumeDynamic extends Aspect {
  EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv);

  JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx);
}
