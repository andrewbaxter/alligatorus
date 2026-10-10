package compilerxyz.main.langhelp;

import compilerxyz.main.artifact.Artifacts;
import compilerxyz.main.meta.Metas;
import compilerxyz.prelude.util.TOrderedMap;

public final class GlobalContext {
    public final Artifacts artifacts;
    public final Compiles compiles;
    public final Sources sources;
    public final Metas metas;
    public boolean captureBytecode;
    public final TOrderedMap<String, byte[]> capturedBytecode = new TOrderedMap<>();

    private GlobalContext(Sources sources, Artifacts artifacts, Compiles compiles, Metas metas) {
        this.sources = sources;
        this.artifacts = artifacts;
        this.compiles = compiles;
        this.metas = metas;
    }

    public static GlobalContext newContext(Sources sources, Artifacts artifacts, Compiles compiles, Metas metas) {
        return new GlobalContext(sources, artifacts, compiles, metas);
    }
}
