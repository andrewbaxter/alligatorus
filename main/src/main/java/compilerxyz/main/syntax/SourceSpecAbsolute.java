package compilerxyz.main.syntax;

public sealed interface SourceSpecAbsolute {
    record Virtual(String name) implements SourceSpecAbsolute {
        @Override
        public String sourceSpecAbsoluteSpecKey() {
            return "virtual:" + name;
        }
    }

    String sourceSpecAbsoluteSpecKey();

    static SourceSpecAbsolute sourceSpecAbsoluteFromKey(String key) {
        if (key.startsWith("local:")) return new Local(key.substring("local:".length()));
        if (key.startsWith("remote:")) {
            var rest = key.substring("remote:".length());
            var subAt = rest.lastIndexOf('!');
            if (subAt < 0) throw new IllegalArgumentException("malformed remote spec key: " + key);
            return new Remote(RemoteOrigin.remoteOriginFromKey(rest.substring(0, subAt)), rest.substring(subAt + 1));
        }
        throw new IllegalArgumentException("not a local or remote spec key: " + key);
    }

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
