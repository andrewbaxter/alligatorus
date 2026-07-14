package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;
import compilerxyz.util.TList;

@BuiltinAspect(uniqueId = 1013L, kind = "Tuple", javadoc = "Identity aspect for tuple")
public interface AspectTuple extends Aspect {
  TList<Type> aspectTupleElementTypes();

  TList<EResult<CodeValue>> aspectTupleTakeElements(ExprContext xx, QExprId loc, Value value);
}
