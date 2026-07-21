package compilerxyz.gen;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1017L)
@BuiltinTypeId(7231L)
public interface AspectSet extends Aspect {
  EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue self_, CodeValue value);
}
