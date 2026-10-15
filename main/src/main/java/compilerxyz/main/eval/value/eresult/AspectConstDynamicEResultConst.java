package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectConstDynamic;

public record AspectConstDynamicEResultConst<T extends Type>(TypeEResultConst<T> type) implements AspectConstDynamic {

    @Override
    public Type aspectConstDynamicDynamicForm(ExprContext xx) {
        return new TypeEResultDynamic<>(TypeForms.dynamicForm(xx, type.elementType));
    }
}
