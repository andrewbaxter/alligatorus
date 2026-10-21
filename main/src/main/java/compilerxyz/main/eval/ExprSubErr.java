package compilerxyz.main.eval;

import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6483L)
public sealed interface ExprSubErr {
    String exprSubErrMessage();

    @BuiltinFull(uniqueId = 6508L)
    record Opaque(String message) implements ExprSubErr {
        @Override
        public String exprSubErrMessage() {
            return message;
        }
    }

    @BuiltinFull(uniqueId = 6509L)
    record ImportFailed(SourceSpecAbsolute spec, String reason) implements ExprSubErr {
        @Override
        public String exprSubErrMessage() {
            return "import of " + spec.sourceSpecAbsoluteSpecKey() + " failed: " + reason;
        }
    }
}
