package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6018L)
@BuiltinPath("types.qexpr_id")
public record QExprId(SourceId source, long expr) {}
