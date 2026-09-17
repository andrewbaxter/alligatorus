package compilerxyz.main.eval.value.unique;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicUniqueDynamic implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicUniqueDynamic INSTANCE = new AspectConsumeDynamicUniqueDynamic();

    private AspectConsumeDynamicUniqueDynamic() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(compilerxyz.main.eval.Unique.class);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
