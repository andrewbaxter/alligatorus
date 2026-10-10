package compilerxyz.main.meta;

import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public interface Metas {
    MetaId metasIntern(ExprContext xx, Meta meta);

    Opt<JsonValue> metasGet(MetaId id);

    TList<ExprMeta> metasForExpr(SourceId source, long expr);

    TList<ScopeEvent> metasScopeEvents(CompileId compile);

    record ExprMetaRow(QExprId loc, MetaId meta) {}

    record ExprMeta(CompileId compile, MetaId meta) {}

    enum ScopeEventKind {
        ENTER,
        BEGIN,
        END,
        PUSH,
        SET,
        POP
    }

    record ScopeEvent(long seq, QExprId loc, ScopeEventKind kind, String name, Opt<MetaId> meta) {}
}
