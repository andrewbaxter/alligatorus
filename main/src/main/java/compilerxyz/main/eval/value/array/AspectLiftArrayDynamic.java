package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;
import java.lang.reflect.Array;

public record AspectLiftArrayDynamic<T extends Type>(TypeArrayDynamic<T> type, AspectLift elementLift)
        implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var length = Array.getLength(externalHostValue);
        var elements = new TList<Value>(length);
        for (int i = 0; i < length; i++) {
            var liftRes = elementLift.aspectLiftLift(xx, loc, Array.get(externalHostValue, i));
            if (liftRes.hasErrors()) return liftRes.reerr();
            elements.add(liftRes.requireValue("TypeArrayDynamic::lift element"));
        }
        return EResult.ok(new ValueArrayConst<>(new TypeArrayConst<>(type.elementType), elements));
    }
}
