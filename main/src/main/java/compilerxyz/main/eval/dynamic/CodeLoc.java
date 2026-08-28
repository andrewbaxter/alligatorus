package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.QExprId;

public sealed interface CodeLoc {
    record Some(QExprId qExprId) implements CodeLoc {}

    record Infallible() implements CodeLoc {
        public static final Infallible INSTANCE = new Infallible();
    }
}
