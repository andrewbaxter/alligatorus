package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectConsumeDynamicMapConst<K extends Type, V extends Type>(TypeMapConst<K, V> type)
        implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TOrderedMap.class));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return ConstHost.loadDynamic(
                loc,
                cv,
                aspectConsumeDynamicJvmClass(xx),
                new TypeMapDynamic<>(
                        TypeForms.dynamicForm(xx, type.keyType), TypeForms.dynamicForm(xx, type.valueType)));
    }
}
