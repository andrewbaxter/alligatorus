package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectConstDynamic;

public record AspectConstDynamicMapConst<K extends Type, V extends Type>(TypeMapConst<K, V> type)
        implements AspectConstDynamic {

    @Override
    public Type aspectConstDynamicDynamicForm(ExprContext xx) {
        return new TypeMapDynamic<>(TypeForms.dynamicForm(xx, type.keyType), TypeForms.dynamicForm(xx, type.valueType));
    }
}
