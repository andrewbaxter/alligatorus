package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;

public record AspectStackValueDynamicTuple(Tuple type) implements AspectStackValueDynamic {

    @Override
    public CodeValue aspectStackValueDynamicToValue(
            ExprContext xx, QExprId loc, CodeDynamic code, ValueDynamicSource source) {
        return new CodeValue(
                Code.target(code),
                EvalOutcome.ofValue(new Tuple(Opt.some(new TupleBaseDynamic(source)), type.elements)));
    }
}
