package compilerxyz.main.syntax;

import java.util.regex.Pattern;

public sealed interface RemoteOrigin {
    record Http(String url, HashValue hash) implements RemoteOrigin {
        @Override
        public String remoteOriginKey() {
            return "http:" + url + "#" + hash.hashValueHashKey();
        }

        @Override
        public String remoteOriginDesc() {
            return url;
        }
    }

    record Git(String url, String commit) implements RemoteOrigin {
        public static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40}");

        @Override
        public String remoteOriginKey() {
            return "git:" + url + "@" + commit;
        }

        @Override
        public String remoteOriginDesc() {
            return url + "@" + commit;
        }
    }

    String remoteOriginKey();

    String remoteOriginDesc();

    static RemoteOrigin remoteOriginFromKey(String key) {
        if (key.startsWith("http:")) {
            var rest = key.substring("http:".length());
            var hashAt = rest.lastIndexOf('#');
            if (hashAt < 0) throw new IllegalArgumentException("malformed http origin key: " + key);
            return new Http(rest.substring(0, hashAt), HashValue.hashValueFromKey(rest.substring(hashAt + 1)));
        }
        if (key.startsWith("git:")) {
            var rest = key.substring("git:".length());
            var commitAt = rest.lastIndexOf('@');
            if (commitAt < 0) throw new IllegalArgumentException("malformed git origin key: " + key);
            return new Git(rest.substring(0, commitAt), rest.substring(commitAt + 1));
        }
        throw new IllegalArgumentException("unknown remote origin key: " + key);
    }
}
