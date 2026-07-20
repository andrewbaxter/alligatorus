package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.annotations.SourceInterface;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.util.Opt;

@Builtin(uniqueId = 1023L)
@compilerxyz.annotations.BuiltinTypeId(7203L)
@SourceInterface(uniqueId = 2010L, scopeId = 3011)
public interface AspectBindDynamic extends Aspect {
  @SourceExposed
  EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv);
}
