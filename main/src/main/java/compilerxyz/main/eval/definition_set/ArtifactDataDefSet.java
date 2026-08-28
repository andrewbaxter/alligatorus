package compilerxyz.main.eval.definition_set;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSet(
        DefSetId defSetId,
        TList<Artifact> dependencies,
        TList<Artifact> functions,
        TList<Artifact> interfaces,
        TList<Artifact> objects,
        TList<Artifact> unions)
        implements SafeJsonSerializable {}
