package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
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
public interface AspectBindDynamic extends Aspect {
  EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv);
}
