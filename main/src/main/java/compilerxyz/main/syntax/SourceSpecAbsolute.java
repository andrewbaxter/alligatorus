package compilerxyz.main.syntax;

import java.util.HexFormat;

public sealed interface SourceSpecAbsolute {
    record Virtual(String name) implements SourceSpecAbsolute {
        @Override
        public String sourceSpecAbsoluteSpecKey() {
            return "virtual:" + name;
        }
    }

    String sourceSpecAbsoluteSpecKey();

    static SourceSpecAbsolute sourceSpecAbsoluteParseKey(String key) {
        if (key.startsWith("virtual:")) return new Virtual(key.substring("virtual:".length()));
        if (key.startsWith("local:")) return new Local(key.substring("local:".length()));
        if (key.startsWith("remote:")) {
            var rest = key.substring("remote:".length());
            var bang = rest.lastIndexOf('!');
            if (bang < 0) throw new IllegalArgumentException("remote spec key without subpath separator: " + key);
            var originKey = rest.substring(0, bang);
            RemoteOrigin origin;
            if (originKey.startsWith("http:")) {
                var url = originKey.substring("http:".length());
                var hash = url.lastIndexOf('#');
                if (hash < 0) throw new IllegalArgumentException("http origin key without hash: " + key);
                var hashKey = url.substring(hash + 1);
                if (!hashKey.startsWith("sha256:")) throw new IllegalArgumentException("not a hash key: " + hashKey);
                origin = new RemoteOrigin.Http(
                        url.substring(0, hash),
                        new HashValue.Sha256(HexFormat.of().parseHex(hashKey.substring("sha256:".length()))));
            } else if (originKey.startsWith("git:")) {
                var url = originKey.substring("git:".length());
                var at = url.lastIndexOf('@');
                if (at < 0) throw new IllegalArgumentException("git origin key without commit: " + key);
                origin = new RemoteOrigin.Git(url.substring(0, at), url.substring(at + 1));
            } else {
                throw new IllegalArgumentException("not a remote origin key: " + key);
            }
            return new Remote(origin, rest.substring(bang + 1));
        }
        throw new IllegalArgumentException("not a source spec key: " + key);
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
