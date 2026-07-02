package compilerxyz.eval.definition_set;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.TList;

public record ArtifactDataDefSet(
    DefSetId defSetId,
    TList<Artifact> dependencies,
    TList<Artifact> functions,
    TList<Artifact> interfaces,
    TList<Artifact> objects) implements SafeJsonSerializable {}
