package compilerxyz.main.eval.value;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1067L)
@BuiltinPath("types.value_dynamic")
public record ValueDynamic(ValueDynamicSource source, Type type) implements Value {
    public Type valueType() {
        return type;
    }
}
