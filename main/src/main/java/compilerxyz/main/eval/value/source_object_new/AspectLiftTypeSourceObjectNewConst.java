package compilerxyz.main.eval.value.source_object_new;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;

public record AspectLiftTypeSourceObjectNewConst(TypeTypeSourceObjectNewConst type) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return EResult.ok(new ValueEmpty(new TypeSourceObjectNewDynamic(type.type())));
    }
}
