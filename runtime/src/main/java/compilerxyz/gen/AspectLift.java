package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;
import org.jspecify.annotations.Nullable;

@BuiltinAspect(
    uniqueId = 1026L,
    kind = "Lift",
    javadoc = "Called to turn a dynamic host-runtime value back into a typed Value.")
@compilerxyz.annotations.BuiltinTypeId(7220L)
public interface AspectLift extends Aspect {
  EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, @Nullable Object externalHostValue);
}
