package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinPath("types.target_bind_sub")
@BuiltinFull(uniqueId = 6350L)
public final class TargetBindSub {
    public final CodeDynamic data;
    public final Type type;

    public TargetBindSub(CodeDynamic data, Type type) {
        this.data = data;
        this.type = type;
    }
}
