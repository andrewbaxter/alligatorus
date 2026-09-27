package compilerxyz.main.syntax;

public sealed interface SourceLiteral {
    record Number(double value) implements SourceLiteral {}

    record Str(String value) implements SourceLiteral {}

    record Bool(boolean value) implements SourceLiteral {}
}
