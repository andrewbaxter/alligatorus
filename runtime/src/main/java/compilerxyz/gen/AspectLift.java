package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

@BuiltinFull(uniqueId = 1026L, identityId = 4026L)
public interface AspectLift extends Aspect {
    EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue);
}
