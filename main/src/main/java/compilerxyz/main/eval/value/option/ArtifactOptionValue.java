package compilerxyz.main.eval.value.option;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;

public record ArtifactOptionValue(Artifact elementType, Opt<Artifact> payload) implements SafeJsonSerializable {}
