package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;

@BuiltinAspect(uniqueId = 1025L, kind = "StackValueDynamic")
@compilerxyz.annotations.BuiltinTypeId(7232L)
public interface AspectStackValueDynamic extends Aspect {
  CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic self_);
}
