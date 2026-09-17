package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicJvmLong implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicJvmLong INSTANCE = new AspectConsumeDynamicJvmLong();

    private AspectConsumeDynamicJvmLong() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.DESC_LONG;
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
