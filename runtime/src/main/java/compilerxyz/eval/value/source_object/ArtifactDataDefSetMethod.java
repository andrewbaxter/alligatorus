package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record ArtifactDataDefSetMethod(
        String name,
        Artifact argType,
        Artifact retType,
        Opt<String> compiledClassName,
        Opt<byte[]> compiledBytecode,
        TList<ArtifactExternal> capturedExternals) {}
