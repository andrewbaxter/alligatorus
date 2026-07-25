package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.util.Opt;

@Builtin(uniqueId = 1023L)
public interface AspectBindDynamic extends Aspect {
  EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv);
}
