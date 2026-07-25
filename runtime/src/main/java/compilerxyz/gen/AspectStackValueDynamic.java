package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;

@Builtin(uniqueId = 1025L)
public interface AspectStackValueDynamic extends Aspect {
  CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic self_);
}
