package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicFloatConst implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicFloatConst INSTANCE = new AspectConsumeDynamicFloatConst();

    private AspectConsumeDynamicFloatConst() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return Numerics.constConsume(xx, loc, cv, NumericKind.FLOAT);
    }
}
