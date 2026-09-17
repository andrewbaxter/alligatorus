package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicBoolDynamicBox implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicBoolDynamicBox INSTANCE = new AspectConsumeDynamicBoolDynamicBox();

    private AspectConsumeDynamicBoolDynamicBox() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Boolean.class));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
