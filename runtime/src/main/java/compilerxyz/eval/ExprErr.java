package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.util.TList;

@BuiltinFull(uniqueId = 6027L)
public record ExprErr(QExprId loc, TList<ExprSubErr> errors) {}
