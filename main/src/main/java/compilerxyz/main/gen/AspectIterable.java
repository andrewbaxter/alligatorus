package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1027L, identityId = 4027L)
public interface AspectIterable extends Aspect {
    Type aspectIterableElementType();

    Binding aspectIterableLoopBinding(ExprContext xx);

    EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, BindingId elementKey, CodeValue body);
}
