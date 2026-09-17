package compilerxyz.main.eval.value.long_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicLongConst implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicLongConst INSTANCE = new AspectConsumeDynamicLongConst();

    private AspectConsumeDynamicLongConst() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return Numerics.constConsume(xx, loc, cv, NumericKind.LONG);
    }
}
