package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TList;

public record ArtifactListValue(Artifact elementType, TList<Artifact> elements) implements SafeJsonSerializable {}
