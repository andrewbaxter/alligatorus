package compilerxyz.docguard;

import java.util.List;
import java.util.Optional;

public final class DocFileGuard {
    public static final String DOC_MODIFIED = "doc-modified";
    public static final String DOC_ADDED = "doc-added";

    private DocFileGuard() {}

    public static List<Violation> check(String path, Optional<String> base, String current) {
        if (base.isEmpty()) {
            return List.of(new Violation(path, 0, DOC_ADDED, "new tracked doc file (not reverted; review manually)"));
        }
        if (base.get().equals(current)) {
            return List.of();
        }
        return List.of(new Violation(path, 0, DOC_MODIFIED, "tracked doc file modified vs merge-base"));
    }

    public static boolean shouldRevert(Optional<String> base, String current) {
        return base.isPresent() && !base.get().equals(current);
    }
}
