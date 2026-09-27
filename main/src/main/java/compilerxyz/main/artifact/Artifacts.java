package compilerxyz.main.artifact;

import compilerxyz.main.eval.CompileId;
import compilerxyz.prelude.util.Result;

public interface Artifacts {
    Result<ArtifactBody, String> artifactsGet(ArtifactIdSource id);

    ArtifactIdSource artifactsReserveId(CompileId compileId);

    void artifactsSet(ArtifactIdSource id, ArtifactBody body);
}
