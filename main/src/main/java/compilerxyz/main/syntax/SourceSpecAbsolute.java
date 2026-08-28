package compilerxyz.main.syntax;

public sealed interface SourceSpecAbsolute {
    record Virtual(String name) implements SourceSpecAbsolute {}

    record Local(String path) implements SourceSpecAbsolute {
        public String sourceSpecAbsoluteSpecKey() {
            return "local:" + path;
        }
    }

    record Remote(String baseUrl, HashValue hash, String subpath) implements SourceSpecAbsolute {
        public String sourceSpecAbsoluteSpecKey() {
            return "remote:" + baseUrl + "#" + hash.hashValueHashKey() + "!" + subpath;
        }
    }
}
