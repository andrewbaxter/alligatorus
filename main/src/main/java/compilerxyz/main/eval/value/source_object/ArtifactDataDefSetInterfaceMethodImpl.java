package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetInterfaceMethodImpl(
        int interfaceMethodIndex,
        Opt<String> compiledClassName,
        Opt<byte[]> compiledBytecode,
        TList<ArtifactExternal> capturedExternals) {}
