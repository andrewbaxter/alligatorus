package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstListDynamic<T extends Type>(TypeListDynamic<T> type) implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm(ExprContext xx) {
        return new TypeListConst<>(TypeForms.constForm(xx, type.elementType));
    }
}
