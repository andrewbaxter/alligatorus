package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6487L)
public record DefSetId(CompileId compileId, long index) {}
