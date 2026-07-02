package compilerxyz.eval.value.source_object;

import compilerxyz.eval.ArtifactExternal;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataDefSetInterfaceMethodImpl(
    int interfaceMethodIndex,
    @Nullable String compiledClassName,
    @Nullable byte[] compiledBytecode,
    long entryId,
    TList<ArtifactExternal.CapturedEntry> capturedExternals) {}
