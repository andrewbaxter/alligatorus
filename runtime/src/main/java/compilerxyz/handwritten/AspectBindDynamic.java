package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.annotations.SourceInterface;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.util.Opt;

@BuiltinAspect(
    uniqueId = 1023L,
    kind = "BindDynamic",
    javadoc = "Creates code to prepare a value for binding, used by the Dynamic target.")
@SourceInterface(uniqueId = 2010L, scopeId = 3011, fieldPrefix = "abd")
public interface AspectBindDynamic extends Aspect {
  @SourceExposed(name = "bindDynamic")
  EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv);
}
