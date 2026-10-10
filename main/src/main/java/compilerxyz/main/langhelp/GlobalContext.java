package compilerxyz.main.langhelp;

import compilerxyz.main.artifact.Artifacts;
import compilerxyz.main.meta.MetaProbe;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

public final class GlobalContext {
    public final Artifacts artifacts;
    public final Compiles compiles;
    public final Sources sources;
    public final Opt<MetaProbe> metaProbe;
    public boolean captureBytecode;
    public final TOrderedMap<String, byte[]> capturedBytecode = new TOrderedMap<>();

    GlobalContext(Sources sources, Artifacts artifacts, Compiles compiles, Opt<MetaProbe> metaProbe) {
        this.sources = sources;
        this.artifacts = artifacts;
        this.compiles = compiles;
        this.metaProbe = metaProbe;
    }

    public static GlobalContext newContext(Sources sources, Artifacts artifacts, Compiles compiles) {
        return new GlobalContext(sources, artifacts, compiles, Opt.none());
    }
}
