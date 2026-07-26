package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;
import org.jspecify.annotations.Nullable;

@Builtin(uniqueId = 1026L, identityId = 4026L)
public interface AspectLift extends Aspect {
  EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, @Nullable Object externalHostValue);
}
