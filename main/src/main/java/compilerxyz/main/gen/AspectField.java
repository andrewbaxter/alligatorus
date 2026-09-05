package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 1015L, identityId = 4015L)
@BuiltinPath("eval.aspect_field")
public interface AspectField extends Aspect {
    EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key);

    Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key);

    TList<ConstKey> aspectFieldKeys(ExprContext xx);
}
