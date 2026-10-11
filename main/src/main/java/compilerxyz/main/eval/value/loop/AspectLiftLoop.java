package compilerxyz.main.eval.value.loop;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;

public final class AspectLiftLoop implements AspectLift {
    public static final AspectLiftLoop INSTANCE = new AspectLiftLoop();

    private AspectLiftLoop() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return EResult.ok(new ValueObjectConst(new TypeLoop(), new TList<>()));
    }
}
