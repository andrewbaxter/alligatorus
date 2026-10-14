package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.Type;

public sealed interface ListDynamic extends Type permits TypeListBoxedDynamic, TypeListSpecializedDynamic {
    ArtifactSupport.ArtifactIdHolder listDynamicArtifactIdCell();
}
