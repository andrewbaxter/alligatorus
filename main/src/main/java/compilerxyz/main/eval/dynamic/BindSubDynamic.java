package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinPath("types.bind_sub_dynamic")
@BuiltinFull(uniqueId = 6350L)
public record BindSubDynamic(CodeDynamicReg value, Type type) {}
