package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicOptionSpecializedDynamic<T extends Type>(TypeOptionSpecializedDynamic<T> type)
        implements AspectConsumeDynamic {
    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return type.storeDescriptor();
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dynamicConsumeToDynamic(loc, cv);
    }
}
