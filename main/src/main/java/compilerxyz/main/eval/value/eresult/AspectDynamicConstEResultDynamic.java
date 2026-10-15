package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstEResultDynamic<T extends Type>(TypeEResultDynamic<T> type)
        implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm(ExprContext xx) {
        return new TypeEResultConst<>(TypeForms.constForm(xx, type.elementType()));
    }
}
