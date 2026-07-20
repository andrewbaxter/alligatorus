package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.annotations.SourceInterface;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1032L)
@compilerxyz.annotations.BuiltinTypeId(7207L)
@SourceInterface(uniqueId = 2008L, scopeId = 3008)
@BuiltinPath("eval.aspect_drop_dynamic")
public interface AspectDropDynamic extends Aspect {
  @SourceExposed
  EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv);
}
