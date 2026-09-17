package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicDoubleDynamicBox implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicDoubleDynamicBox INSTANCE = new AspectConsumeDynamicDoubleDynamicBox();

    private AspectConsumeDynamicDoubleDynamicBox() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Double.class));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
