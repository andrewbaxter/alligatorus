package compilerxyz.eval.value.object_;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;

public sealed interface TypeObjectCastFromRule {

  boolean matches(ExprContext xx, Type sourceType);

  EResult<CodeValue> apply(
      ExprContext xx, TypeObjectDynamic dest, QExprId loc, CodeValue cv, Type sourceType);

  record RestampFromIdentity(Unique identity) implements TypeObjectCastFromRule {
    @Override
    public boolean matches(ExprContext xx, Type sourceType) {
      return sourceType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public EResult<CodeValue> apply(
        ExprContext xx, TypeObjectDynamic dest, QExprId loc, CodeValue cv, Type sourceType) {
      return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(dest, 1))));
    }
  }
}
