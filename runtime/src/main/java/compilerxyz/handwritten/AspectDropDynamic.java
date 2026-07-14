package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1032L,
    kind = "DropDynamic",
    javadoc = "Allows a value to emit cleanup (drop) code when its scope ends.")
public interface AspectDropDynamic extends Aspect {
  EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv);
}
