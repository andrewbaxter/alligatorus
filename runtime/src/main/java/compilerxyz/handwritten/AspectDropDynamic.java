package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.annotations.SourceInterface;
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
@SourceInterface(uniqueId = 2008L, scopeId = 3008, fieldPrefix = "dd", path = "eval.aspect_drop_dynamic")
public interface AspectDropDynamic extends Aspect {
  @SourceExposed(name = "drop")
  EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv);
}
