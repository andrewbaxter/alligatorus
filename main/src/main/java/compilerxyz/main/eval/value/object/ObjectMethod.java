package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6408L)
public record ObjectMethod(String langName, Type funcType) {}
