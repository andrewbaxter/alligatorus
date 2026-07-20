package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1017L,
    kind = "Set",
    javadoc = "Allows the value to be assigned new data (mutated).")
@compilerxyz.annotations.BuiltinTypeId(7231L)
public interface AspectSet extends Aspect {
  EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue self_, CodeValue value);
}
