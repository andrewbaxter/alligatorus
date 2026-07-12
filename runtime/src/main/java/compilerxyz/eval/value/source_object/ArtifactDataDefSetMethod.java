package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataDefSetMethod(
    String name,
    Artifact argType,
    Artifact retType,
    @Nullable String compiledClassName,
    @Nullable byte[] compiledBytecode,
    TList<ArtifactExternal> capturedExternals) {}
