package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6486L)
public record CompileId(ImportStateId importState, SourceId source) {}
