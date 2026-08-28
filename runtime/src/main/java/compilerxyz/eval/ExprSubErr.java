package compilerxyz.eval;

public sealed interface ExprSubErr {
    record Opaque(String message) implements ExprSubErr {}
}
