package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectConstDynamic;

public record AspectConstDynamicArrayConst<T extends Type>(TypeArrayConst<T> type) implements AspectConstDynamic {

    @Override
    public Type aspectConstDynamicDynamicForm(ExprContext xx) {
        return new TypeArrayDynamic<>(TypeForms.dynamicForm(xx, type.elementType));
    }
}
