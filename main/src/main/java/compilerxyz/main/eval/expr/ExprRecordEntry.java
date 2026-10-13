package compilerxyz.main.eval.expr;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6477L)
@BuiltinPath("types.expr_record_entry")
public record ExprRecordEntry(StagedExprCell key, StagedExprCell value) {}
