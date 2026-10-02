package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactEResultValue(Artifact elementType, Opt<Artifact> ok, TList<Artifact> errs)
        implements SafeJsonSerializable {}
