package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.annotations.SourceInterface;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@Builtin(uniqueId = 1015L)
@compilerxyz.annotations.BuiltinTypeId(7214L)
@SourceInterface(uniqueId = 1030L, scopeId = 3004)
@BuiltinPath("eval.aspect_field")
public interface AspectField extends Aspect {
  @SourceExposed
  EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key);

  @SourceExposed
  Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key);

  TList<ConstKey> aspectFieldKeys(ExprContext xx);
}
