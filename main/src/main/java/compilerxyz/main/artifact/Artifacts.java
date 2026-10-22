package compilerxyz.main.artifact;

import compilerxyz.main.eval.CompileId;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6536L)
@BuiltinPath("eval.artifacts")
public interface Artifacts {
    Opt<ArtifactBody> artifactsGet(ArtifactIdSource id);

    ArtifactIdSource artifactsReserveId(CompileId compileId);

    void artifactsSet(ArtifactIdSource id, ArtifactBody body);
}
