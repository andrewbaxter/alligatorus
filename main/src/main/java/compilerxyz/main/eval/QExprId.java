package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6018L)
public record QExprId(SourceId source, long expr) {}
