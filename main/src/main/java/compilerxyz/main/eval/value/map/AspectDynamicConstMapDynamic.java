package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type)
        implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm(ExprContext xx) {
        return new TypeMapConst<>(TypeForms.constForm(xx, type.keyType), TypeForms.constForm(xx, type.valueType));
    }
}
