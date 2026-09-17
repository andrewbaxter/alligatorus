package compilerxyz.main.eval.value.null_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicNullDynamic implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicNullDynamic INSTANCE = new AspectConsumeDynamicNullDynamic();

    private AspectConsumeDynamicNullDynamic() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && DynUtil.sourceOf(cv.value().optUnwrap()).optIsSome()) {
            return EResult.ok(cv);
        }
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.hasErrors()) return carryRes.reerr();
        var raw = b.constNull();
        return EResult.ok(DynUtil.regToValue(b.code(), raw, new TypeNullDynamic()));
    }
}
