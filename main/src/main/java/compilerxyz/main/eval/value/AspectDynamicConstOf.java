package compilerxyz.main.eval.value;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstOf(Type constForm) implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm() {
        return constForm;
    }
}
