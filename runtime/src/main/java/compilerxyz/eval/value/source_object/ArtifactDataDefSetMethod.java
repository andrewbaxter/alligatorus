package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataDefSetMethod(
    String name,
    String fieldName,
    Artifact argType,
    Artifact retType,
    @Nullable String compiledClassName,
    @Nullable byte[] compiledBytecode,
    long entryId,
    TList<ArtifactExternal> capturedExternals) {}
