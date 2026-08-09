package compilerxyz.gen;


import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@BuiltinFull(uniqueId = 1015L, identityId = 4015L)
@BuiltinPath("eval.aspect_field")
public interface AspectField extends Aspect {
  EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key);

  Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key);

  TList<ConstKey> aspectFieldKeys(ExprContext xx);
}
