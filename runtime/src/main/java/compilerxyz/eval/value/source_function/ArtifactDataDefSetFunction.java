package compilerxyz.eval.value.source_function;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.DefSetId;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataDefSetFunction(
    Artifact defSet,
    DefSetId defSetId,
    @Nullable String name,
    Artifact argType,
    Artifact retType,
    String compiledClassName,
    byte[] compiledBytecode,
    TList<ArtifactExternal> capturedExternals)
    implements SafeJsonSerializable {}
