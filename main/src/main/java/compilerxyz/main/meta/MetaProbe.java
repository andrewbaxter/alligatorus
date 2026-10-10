package compilerxyz.main.meta;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.prelude.util.TList;

public final class MetaProbe {
    public record ScopeEntry(String name, Meta meta) {}

    public record Hit(ExprContext xx, Meta meta, TList<ScopeEntry> scope) {}

    public final QExprId target;
    public final TList<Hit> hits = new TList<>();

    public MetaProbe(QExprId target) {
        this.target = target;
    }
}
