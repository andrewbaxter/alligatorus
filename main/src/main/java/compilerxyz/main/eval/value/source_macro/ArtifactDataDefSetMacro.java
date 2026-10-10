package compilerxyz.main.eval.value.source_macro;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;

public record ArtifactDataDefSetMacro(
        Artifact function, Opt<Artifact> exceptionType, Opt<String> description, Opt<Long> metaId)
        implements SafeJsonSerializable {}
