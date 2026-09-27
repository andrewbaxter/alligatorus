package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6027L)
@BuiltinPath("types.expr_err")
public record ExprErr(QExprId loc, TList<ExprSubErr> errors) {
    public static ExprErr opaque(QExprId loc, String message) {
        return new ExprErr(loc, TList.of(new ExprSubErr.Opaque(message)));
    }

    public String message() {
        var sb = new StringBuilder();
        sb.append("[")
                .append(loc.source().value())
                .append(":")
                .append(loc.expr())
                .append("] ");
        for (var sub : errors) {
            if (!sb.toString().endsWith("] ")) sb.append("; ");
            switch (sub) {
                case ExprSubErr.Opaque o -> sb.append(o.message());
            }
        }
        return sb.toString();
    }
}
