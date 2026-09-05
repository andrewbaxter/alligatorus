package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public record AspectTypeSourceFunctionConst(TypeSourceFunctionConst type) implements AspectType {

    @Override
    public Type aspectTypeType(Value value) {
        return new TypeSourceFunctionConst(type.data());
    }
}
