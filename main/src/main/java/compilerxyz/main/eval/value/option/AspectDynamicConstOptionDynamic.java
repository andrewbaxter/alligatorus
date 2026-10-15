package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstOptionDynamic(OptionDynamic type) implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm(ExprContext xx) {
        return new TypeOptionConst<>(TypeForms.constForm(xx, type.optionDynamicElementType()));
    }
}
