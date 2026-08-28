package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1031L, identityId = 4031L)
public interface AspectPostDeserialize extends Aspect {
    EResult<CodeValue> aspectPostDeserializePostDeserialize(ExprContext xx, QExprId loc, CodeValue cv);
}
