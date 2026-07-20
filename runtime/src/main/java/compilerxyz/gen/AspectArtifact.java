package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1028L)
@compilerxyz.annotations.BuiltinTypeId(7200L)
public interface AspectArtifact extends Aspect {
  ArtifactExporter aspectArtifactExporter();
}
