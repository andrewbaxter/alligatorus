package compilerxyz.eval.value.list;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Type;

public sealed interface ListDynamic extends Type, ExporterAny
    permits TypeListBoxedDynamic, TypeListSpecializedDynamic {
  ArtifactSupport.ArtifactIdHolder listDynamicArtifactIdCell();
}
