package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6018L)
@BuiltinPath("types.qexpr_id")
public record QExprId(SourceId source, Opt<Long> expr, Opt<QExprId> site) {
    public static QExprId root(SourceId source, Opt<Long> expr) {
        return new QExprId(source, expr, Opt.none());
    }

    public QExprId withSite(Opt<QExprId> site) {
        return new QExprId(source, expr, site);
    }
}
