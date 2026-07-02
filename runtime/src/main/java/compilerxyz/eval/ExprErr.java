package compilerxyz.eval;

import compilerxyz.util.TList;

public record ExprErr(QExprId loc, TList<ExprSubErr> errors) {}
