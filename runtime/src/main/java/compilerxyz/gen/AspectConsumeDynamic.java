package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.util.Opt;

@BuiltinFull(uniqueId = 1024L, identityId = 4024L)
public interface AspectConsumeDynamic extends Aspect {
  EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv);

  JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx);

}
