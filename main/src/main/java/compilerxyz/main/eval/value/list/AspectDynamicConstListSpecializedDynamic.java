package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstListSpecializedDynamic<T extends Type>(TypeListSpecializedDynamic<T> type)
        implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm() {
        return new TypeListConst(type.elementType);
    }
}
