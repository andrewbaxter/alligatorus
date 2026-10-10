package compilerxyz.main.meta;

import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.prelude.util.TList;

public interface Metas {
    TList<ExprFields> metasExprFields(SourceId source, long expr);

    TList<ScopeEvent> metasScopeEvents(CompileId compile);

    record ExprFieldsRow(QExprId loc, TList<String> names) {}

    record ExprFields(CompileId compile, TList<String> names) {}

    enum ScopeEventKind {
        ENTER,
        BEGIN,
        END,
        PUSH,
        SET,
        POP
    }

    record ScopeEvent(long seq, QExprId loc, ScopeEventKind kind, String name) {}
}
