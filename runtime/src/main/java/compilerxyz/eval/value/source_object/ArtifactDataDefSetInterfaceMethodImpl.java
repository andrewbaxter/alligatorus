package compilerxyz.eval.value.source_object;

import compilerxyz.eval.ArtifactExternal;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record ArtifactDataDefSetInterfaceMethodImpl(
        int interfaceMethodIndex,
        Opt<String> compiledClassName,
        Opt<byte[]> compiledBytecode,
        TList<ArtifactExternal> capturedExternals) {}
