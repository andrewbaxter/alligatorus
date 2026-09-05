package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstListBoxedDynamic<T extends Type>(TypeListBoxedDynamic<T> type)
        implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm() {
        return new TypeListConst(type.elementType);
    }
}
