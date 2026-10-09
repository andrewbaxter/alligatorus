package compilerxyz.main.eval;

import compilerxyz.main.syntax.SourceSpecAbsolute;

public sealed interface ExprSubErr {
    String exprSubErrMessage();

    record Opaque(String message) implements ExprSubErr {
        @Override
        public String exprSubErrMessage() {
            return message;
        }
    }

    record ImportFailed(SourceSpecAbsolute spec, String reason) implements ExprSubErr {
        @Override
        public String exprSubErrMessage() {
            return "import of " + spec.sourceSpecAbsoluteSpecKey() + " failed: " + reason;
        }
    }
}
