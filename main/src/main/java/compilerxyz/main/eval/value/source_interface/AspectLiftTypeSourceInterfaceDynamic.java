package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftTypeSourceInterfaceDynamic implements AspectLift {
    public static final AspectLiftTypeSourceInterfaceDynamic INSTANCE = new AspectLiftTypeSourceInterfaceDynamic();

    private AspectLiftTypeSourceInterfaceDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var host = (TypeSourceInterfaceDynamic) externalHostValue.optUnwrap();
        return EResult.ok(new ValueObjectConst(new TypeTypeSourceInterfaceDynamic(Opt.some(host)), host));
    }
}
