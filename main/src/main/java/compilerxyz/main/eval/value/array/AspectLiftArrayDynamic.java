package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;

public record AspectLiftArrayDynamic<T extends Type>(TypeArrayDynamic<T> type) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        if (!externalHostValue.getClass().isArray()) {
            return EResult.err(
                    loc,
                    "TypeArrayDynamic::lift: expected an array, got "
                            + externalHostValue.getClass().getName());
        }
        return EResult.ok(new ValueObjectConst(new TypeArrayConst<>(type.elementType), externalHostValue));
    }
}
