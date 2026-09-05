package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public record AspectLiftSourceObjectDynamic(TypeSourceObjectDynamic type) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        return EResult.ok(new ValueObjectConst(new TypeSourceObjectConst(type), externalHostValue.optUnwrapOr(null)));
    }
}
