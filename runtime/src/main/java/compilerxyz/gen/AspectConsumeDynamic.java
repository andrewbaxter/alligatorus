package compilerxyz.gen;

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
@compilerxyz.annotations.BuiltinTypeId(7206L)
@SourceInterface(uniqueId = 2009L, scopeId = 3010)
public interface AspectConsumeDynamic extends Aspect {
  @SourceExposed
  EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv);

  @SourceExposed
  JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx);
}
