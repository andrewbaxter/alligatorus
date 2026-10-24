package compilerxyz.main.eval;

import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.prelude.util.TOrderedMap;

public final class ArtifactImportScope {
    public final TOrderedMap<ArtifactIdSource, Object> imported = new TOrderedMap<>();

    public int depth = 0;

    public static ArtifactImportScope of(CompileImportContext ix) {
        return ix.artifactImportScope;
    }
}
