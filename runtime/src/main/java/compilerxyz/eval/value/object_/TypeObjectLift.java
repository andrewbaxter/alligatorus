package compilerxyz.eval.value.object_;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Value;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface TypeObjectLift {
  EResult<Value> typeObjectLift(ExprContext xx, QExprId loc, @Nullable Object host);
}
