package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicArrayConst<T extends Type>(TypeArrayConst<T> type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JvmSort.descriptor(xx, type.elementType).arrayOf();
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return ConstHost.loadDynamic(
                loc, cv, aspectConsumeDynamicJvmClass(xx), new TypeArrayDynamic<>(type.elementType));
    }
}
