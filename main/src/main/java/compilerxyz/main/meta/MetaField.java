package compilerxyz.main.meta;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

public record MetaField(JsonValue data, Opt<Function<ExprContext, Meta>> meta) {}
