package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1028L)
public interface AspectArtifact extends Aspect {
  ArtifactExporter aspectArtifactExporter();
}
