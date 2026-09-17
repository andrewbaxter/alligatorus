package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.BindSubDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 1023L, identityId = 4023L)
public interface AspectBindDynamic extends Aspect {
    EResult<Opt<BindSubDynamic>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv);
}
