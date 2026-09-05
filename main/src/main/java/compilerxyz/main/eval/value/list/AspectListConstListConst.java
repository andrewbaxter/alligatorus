package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectListConst;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;

public record AspectListConstListConst<T extends Type>(TypeListConst<T> type) implements AspectListConst {

    @Override
    public Type aspectListConstElementType(Value value) {
        return type.elementType;
    }

    @Override
    public TList<Value> aspectListConstElements(Value value) {
        return TypeListConst.elementsOf(value);
    }
}
