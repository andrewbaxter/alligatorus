package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.eval.aspect.Aspect;

@BuiltinAspect(
    uniqueId = 1028L,
    kind = "Artifact",
    javadoc = "Allows a value to be exported from a module.")
@compilerxyz.annotations.BuiltinTypeId(7200L)
public interface AspectArtifact extends Aspect {
  ArtifactExporter aspectArtifactExporter();
}
