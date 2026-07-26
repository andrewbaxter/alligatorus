package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.util.Opt;

@Builtin(uniqueId = 1024L, identityId = 4024L)
public interface AspectConsumeDynamic extends Aspect {
  EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv);

  JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx);

  default CodeValue aspectConsumeDynamicReturn(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof ValueDynamic vd)) {
      return cv;
    }
    var desc = aspectConsumeDynamicJvmClass(xx).value();
    var width = desc.equals("J") || desc.equals("D") ? 2 : 1;
    return switch (vd.source()) {
      case ValueDynamicSource.Expr ignored -> cv;
      case ValueDynamicSource.Binding ignored ->
          new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(vd.type_(), width)));
      case ValueDynamicSource.Field ignored ->
          new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(vd.type_(), width)));
    };
  }
}
