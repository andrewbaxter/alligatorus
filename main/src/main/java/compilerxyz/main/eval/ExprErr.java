package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6027L)
public record ExprErr(QExprId loc, TList<ExprSubErr> errors) {}
