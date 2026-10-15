package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectConstDynamic;
import compilerxyz.main.gen.GenOptions;

public record AspectConstDynamicOptionConst<T extends Type>(TypeOptionConst<T> type) implements AspectConstDynamic {

    @Override
    public Type aspectConstDynamicDynamicForm(ExprContext xx) {
        return GenOptions.optionDynamic(xx, TypeForms.dynamicForm(xx, type.elementType));
    }
}
