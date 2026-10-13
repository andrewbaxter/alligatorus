package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenOptions;

public record AspectConsumeDynamicOptionConst<T extends Type>(TypeOptionConst<T> type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return GenAspectConsumeDynamic.getMaybe(xx, GenOptions.optionDynamic(xx, type.elementType))
                .optUnwrap()
                .aspectConsumeDynamicJvmClass(xx);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return ConstHost.loadDynamic(
                loc, cv, aspectConsumeDynamicJvmClass(xx), GenOptions.optionDynamic(xx, type.elementType));
    }
}
