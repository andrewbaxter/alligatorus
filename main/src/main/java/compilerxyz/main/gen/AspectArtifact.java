package compilerxyz.main.gen;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 1028L, identityId = 4028L)
@BuiltinPath("eval.aspect_artifact")
public interface AspectArtifact extends Aspect {
    ArtifactExporter aspectArtifactExporter();
}
