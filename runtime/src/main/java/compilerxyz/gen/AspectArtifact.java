package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.eval.aspect.Aspect;

@BuiltinFull(uniqueId = 1028L, identityId = 4028L)
public interface AspectArtifact extends Aspect {
    ArtifactExporter aspectArtifactExporter();
}
