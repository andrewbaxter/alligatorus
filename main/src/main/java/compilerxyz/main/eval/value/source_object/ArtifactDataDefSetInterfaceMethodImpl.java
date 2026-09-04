package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetInterfaceMethodImpl(
        Artifact interfaceMethod, String jvmMethodName, TList<ArtifactExternal> capturedExternals) {}
