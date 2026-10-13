package compilerxyz.main.eval.value.empty;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;

public record ValueEmpty(Type type) implements Value {
    public Type valueType() {
        return type;
    }
}
