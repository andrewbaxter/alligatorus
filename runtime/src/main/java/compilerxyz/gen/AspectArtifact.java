package compilerxyz.gen;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1028L)
@BuiltinTypeId(7200L)
public interface AspectArtifact extends Aspect {
  ArtifactExporter aspectArtifactExporter();
}
