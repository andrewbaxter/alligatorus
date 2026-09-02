package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.Type;

public sealed interface ListDynamic extends Type, Artifactable
        permits TypeListBoxedDynamic, TypeListSpecializedDynamic {
    ArtifactSupport.ArtifactIdHolder listDynamicArtifactIdCell();
}
