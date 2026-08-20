package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1027L, identityId = 4027L)
public interface AspectIterable extends Aspect {
  Type aspectIterableElementType();

  Binding aspectIterableLoopBinding(ExprContext xx);

  EResult<CodeValue> aspectIterableForeach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementKey, CodeValue body);
}
