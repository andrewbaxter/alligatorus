package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetMethod(
        String name,
        Artifact argType,
        Artifact retType,
        Opt<String> compiledClassName,
        Opt<byte[]> compiledBytecode,
        TList<ArtifactExternal> capturedExternals) {}
