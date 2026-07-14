package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@BuiltinAspect(
    uniqueId = 1015L,
    kind = "Field",
    javadoc = "Allows field expressions to work on the value of this type")
public interface AspectField extends Aspect {
  EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key);

  Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key);

  TList<ConstKey> aspectFieldKeys(ExprContext xx);
}
