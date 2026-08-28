package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.eval.Type;

public sealed interface ListDynamic extends Type, ExporterAny permits TypeListBoxedDynamic, TypeListSpecializedDynamic {
    ArtifactSupport.ArtifactIdHolder listDynamicArtifactIdCell();
}
