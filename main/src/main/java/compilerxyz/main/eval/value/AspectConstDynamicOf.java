package compilerxyz.main.eval.value;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectConstDynamic;

public record AspectConstDynamicOf(Type dynamicForm) implements AspectConstDynamic {

    @Override
    public Type aspectConstDynamicDynamicForm(ExprContext xx) {
        return dynamicForm;
    }
}
