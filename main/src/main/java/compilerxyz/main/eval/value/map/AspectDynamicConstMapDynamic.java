package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectDynamicConst;

public record AspectDynamicConstMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type)
        implements AspectDynamicConst {

    @Override
    public Type aspectDynamicConstConstForm() {
        return new TypeMapConst(type.keyType, type.valueType);
    }
}
