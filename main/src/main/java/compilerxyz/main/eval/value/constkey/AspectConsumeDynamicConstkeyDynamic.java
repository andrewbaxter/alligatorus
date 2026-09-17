package compilerxyz.main.eval.value.constkey;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicConstkeyDynamic implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicConstkeyDynamic INSTANCE = new AspectConsumeDynamicConstkeyDynamic();

    private AspectConsumeDynamicConstkeyDynamic() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(compilerxyz.main.eval.ConstKey.class);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
