package compilerxyz.main.eval.value.ulong;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicUlongConst implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicUlongConst INSTANCE = new AspectConsumeDynamicUlongConst();

    private AspectConsumeDynamicUlongConst() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return Numerics.constConsume(xx, loc, cv, NumericKind.ULONG);
    }
}
