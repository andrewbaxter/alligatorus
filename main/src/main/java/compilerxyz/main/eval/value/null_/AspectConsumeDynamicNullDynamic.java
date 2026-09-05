package compilerxyz.main.eval.value.null_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
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
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        var dyn = dynCast.requireValue("dyn");
        if (dyn.optIsSome()) {
            return EResult.ok(dyn.optUnwrap());
        }
        return EResult.ok(JavaBytecodeUtils.literalNull(loc));
    }
}
