package compilerxyz.main.eval.value.byte_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionByteToStringDynamic implements AspectFunction {
    public static final AspectFunctionByteToStringDynamic INSTANCE = new AspectFunctionByteToStringDynamic();

    private AspectFunctionByteToStringDynamic() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var selfRes = DynUtil.valueReg(xx, loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var out = b.invokeStatic(
                GenCodegenDescriptors.NUMERIC_FORMAT_FORMAT_BYTE, b.reg(selfRes.requireValue("selfDyn")));
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeStringDynamic()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
