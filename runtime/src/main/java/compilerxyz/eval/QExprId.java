package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6018L)
public record QExprId(SourceId source, long expr) {}
