package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Expr;

public record IfBranch(CodeValue condition, Expr body) {}
