package compilerxyz.main.eval.value;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenAspectConstDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;

public final class TypeForms {
    private TypeForms() {}

    public static Type constForm(ExprContext xx, Type type) {
        var aspect = GenAspectDynamicConst.getMaybe(xx, type);
        return aspect.optIsSome() ? aspect.optUnwrap().aspectDynamicConstConstForm(xx) : type;
    }

    public static Type dynamicForm(ExprContext xx, Type type) {
        var aspect = GenAspectConstDynamic.getMaybe(xx, type);
        return aspect.optIsSome() ? aspect.optUnwrap().aspectConstDynamicDynamicForm(xx) : type;
    }
}
