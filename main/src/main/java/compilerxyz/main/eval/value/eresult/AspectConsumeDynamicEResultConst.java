package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicEResultConst<T extends Type>(TypeEResultConst<T> type)
        implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return ConstHost.loadDynamic(
                loc, cv, aspectConsumeDynamicJvmClass(xx), new TypeEResultDynamic<>(type.elementType));
    }
}
