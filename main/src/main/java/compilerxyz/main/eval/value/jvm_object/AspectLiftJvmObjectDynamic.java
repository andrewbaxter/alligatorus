package compilerxyz.main.eval.value.jvm_object;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;

public final class AspectLiftJvmObjectDynamic implements AspectLift {
    public static final AspectLiftJvmObjectDynamic INSTANCE = new AspectLiftJvmObjectDynamic();

    private AspectLiftJvmObjectDynamic() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        return EResult.ok(new ValueObjectConst(new TypeJvmObjectConst(), externalHostValue));
    }
}
