package compilerxyz.main.gen;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1026L, identityId = 4026L)
public interface AspectLift extends Aspect {
    EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue);
}
