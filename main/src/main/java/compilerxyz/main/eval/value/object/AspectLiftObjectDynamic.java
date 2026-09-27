package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;

public record AspectLiftObjectDynamic(TypeObjectDynamic type) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return EResult.ok(new ValueObjectConst(type.pairedConst(), externalHostValue));
    }
}
