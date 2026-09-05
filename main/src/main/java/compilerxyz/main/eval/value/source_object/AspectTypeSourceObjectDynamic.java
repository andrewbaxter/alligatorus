package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public record AspectTypeSourceObjectDynamic(TypeSourceObjectDynamic type) implements AspectType {

    @Override
    public Type aspectTypeType(Value value) {
        return type;
    }
}
