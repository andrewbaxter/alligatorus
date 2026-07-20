package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1035L,
    kind = "DropDynamicLight",
    javadoc = "Light drop for values that only need to clean up their stack footprint (POP) when discarded.")
@compilerxyz.annotations.BuiltinTypeId(7208L)
public interface AspectDropDynamicLight extends Aspect {
  EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv);
}
