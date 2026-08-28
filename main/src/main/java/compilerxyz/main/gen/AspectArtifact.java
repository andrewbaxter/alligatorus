package compilerxyz.main.gen;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1028L, identityId = 4028L)
public interface AspectArtifact extends Aspect {
    ArtifactExporter aspectArtifactExporter();
}
