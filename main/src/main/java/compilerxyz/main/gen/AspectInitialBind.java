package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingSlot;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1021L, identityId = 4021L)
public interface AspectInitialBind extends Aspect {
    EResult<Binding> aspectInitialBindInitialBind(ExprContext xx, QExprId loc, BindingSlot slot);
}
