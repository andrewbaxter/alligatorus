package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public final class AspectLiftCodeDynamic implements AspectLift {
    public static final AspectLiftCodeDynamic INSTANCE = new AspectLiftCodeDynamic();

    private AspectLiftCodeDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        if (externalHostValue.optIsNone() || !(externalHostValue.optUnwrap() instanceof Code codeObj)) {
            return EResult.err(loc, "TypeCodeDynamic::lift: expected host Code");
        }
        return EResult.ok(new ValueObjectConst(new TypeCodeDynamic(), codeObj));
    }
}
