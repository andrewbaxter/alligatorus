package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.annotations.SourceInterface;
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
@SourceInterface(uniqueId = 2009L, scopeId = 3010)
public interface AspectConsumeDynamic extends Aspect {
  @SourceExposed(name = "toDynamic")
  EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv);

  @SourceExposed(name = "consumeDynamicJvmClass")
  JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx);
}
