package compilerxyz.syntax;

public sealed interface SourceSpecAbsolute {
  record Virtual(String path) implements SourceSpecAbsolute {}
}
