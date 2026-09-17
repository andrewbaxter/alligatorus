package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicFloatDynamic implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicFloatDynamic INSTANCE = new AspectConsumeDynamicFloatDynamic();

    private AspectConsumeDynamicFloatDynamic() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.DESC_FLOAT;
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
