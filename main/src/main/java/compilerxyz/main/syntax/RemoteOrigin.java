package compilerxyz.main.syntax;

import compilerxyz.prelude.annotations.BuiltinFull;
import java.util.regex.Pattern;

@BuiltinFull(uniqueId = 6526L)
public sealed interface RemoteOrigin {
    @BuiltinFull(uniqueId = 6527L)
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

    @BuiltinFull(uniqueId = 6528L)
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
