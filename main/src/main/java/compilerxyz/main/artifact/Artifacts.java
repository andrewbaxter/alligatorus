package compilerxyz.main.artifact;

import compilerxyz.prelude.util.Result;

public interface Artifacts {
    Result<ArtifactBody, String> artifactsGet(ArtifactIdSource id);

    ArtifactIdSource artifactsReserveId();

    void artifactsSet(ArtifactIdSource id, ArtifactBody body);
}
