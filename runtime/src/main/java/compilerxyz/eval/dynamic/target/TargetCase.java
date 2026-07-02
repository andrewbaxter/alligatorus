package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.Expr;
import compilerxyz.eval.Type;

/**
 * One case of {@code Target::match_}: dynamic tag string, payload type, and per-case body expr.
 * Port of the Rust tuple {@code (String, Rc<dyn Type>, &Expr)} threaded through
 * {@code Target::match_}.
 */
public record TargetCase(String dynamicTag, Type variantType, Expr body) {}
