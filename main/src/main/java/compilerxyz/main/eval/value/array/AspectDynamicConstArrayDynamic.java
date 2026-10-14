package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstArrayDynamic<T extends Type>(TypeArrayDynamic<T> type) implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm(ExprContext xx) {
        return new TypeArrayConst<>(TypeForms.constForm(xx, type.elementType));
    }
}
