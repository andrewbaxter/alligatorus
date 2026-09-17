package compilerxyz.main.eval.dynamic;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6451L)
public record CodeDynamicReg(CodeDynamic code, JavaBytecodeReg reg) {}
