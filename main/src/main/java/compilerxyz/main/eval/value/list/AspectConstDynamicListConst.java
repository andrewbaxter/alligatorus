package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectConstDynamic;
import compilerxyz.main.gen.GenLists;

public record AspectConstDynamicListConst<T extends Type>(TypeListConst<T> type) implements AspectConstDynamic {

    @Override
    public Type aspectConstDynamicDynamicForm(ExprContext xx) {
        return GenLists.listDynamic(xx, TypeForms.dynamicForm(xx, type.elementType));
    }
}
