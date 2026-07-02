package compilerxyz.eval.value.source_exports;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.safejson.SafeJsonSerializable;

public record ArtifactDataDefSetExportsLifted(Artifact defSet, DefSetId defSetId)
    implements SafeJsonSerializable {}
