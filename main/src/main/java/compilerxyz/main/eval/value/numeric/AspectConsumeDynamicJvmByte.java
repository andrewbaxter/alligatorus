package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicJvmByte implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicJvmByte INSTANCE = new AspectConsumeDynamicJvmByte();

    private AspectConsumeDynamicJvmByte() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.DESC_BYTE;
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
