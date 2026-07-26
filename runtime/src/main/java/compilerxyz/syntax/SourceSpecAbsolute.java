package compilerxyz.syntax;

public sealed interface SourceSpecAbsolute {
  record Virtual(String name) implements SourceSpecAbsolute {}

  record Local(String path) implements SourceSpecAbsolute {}

  record Remote(String baseUrl, HashValue hash, String subpath) implements SourceSpecAbsolute {}

  default String specKey() {
    return switch (this) {
      case Virtual v -> "virtual:" + v.name();
      case Local l -> "local:" + l.path();
      case Remote r -> "remote:" + r.baseUrl() + "#" + r.hash().hashKey() + "!" + r.subpath();
    };
  }
}
