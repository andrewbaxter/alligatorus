package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.Expr;
import compilerxyz.eval.Type;

public record TargetCase(String dynamicTag, Type variantType, Expr body) {
}
