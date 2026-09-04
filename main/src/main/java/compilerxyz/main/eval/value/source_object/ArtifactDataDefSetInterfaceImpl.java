package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetInterfaceImpl(
        Artifact iface, TList<ArtifactDataDefSetInterfaceMethodImpl> methodImpls) {}
