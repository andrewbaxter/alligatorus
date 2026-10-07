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
}
