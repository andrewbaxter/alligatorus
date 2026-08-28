package compilerxyz.syntax;

public sealed interface SourceLiteral {
    record Null() implements SourceLiteral {}

    record Number(double value) implements SourceLiteral {}

    record Str(String value) implements SourceLiteral {}
}
