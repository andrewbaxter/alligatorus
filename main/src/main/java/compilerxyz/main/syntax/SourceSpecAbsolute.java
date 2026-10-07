package compilerxyz.main.syntax;

public sealed interface SourceSpecAbsolute {
    record Virtual(String name) implements SourceSpecAbsolute {
        @Override
        public String sourceSpecAbsoluteSpecKey() {
            return "virtual:" + name;
        }
    }

    String sourceSpecAbsoluteSpecKey();

    record Local(String path) implements SourceSpecAbsolute {
        @Override
        public String sourceSpecAbsoluteSpecKey() {
            return "local:" + path;
        }
    }

    record Remote(RemoteOrigin origin, String subpath) implements SourceSpecAbsolute {
        @Override
        public String sourceSpecAbsoluteSpecKey() {
            return "remote:" + origin.remoteOriginKey() + "!" + subpath;
        }
    }
}
