package compilerxyz.main.artifact;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6538L)
@BuiltinPath("types.artifact_body_exporter")
public sealed interface ArtifactBodyExporter {
    @BuiltinFull(uniqueId = 6539L)
    @BuiltinPath("types.artifact_body_exporter_body")
    record Body(ArtifactBody inner) implements ArtifactBodyExporter {}

    @BuiltinFull(uniqueId = 6540L)
    @BuiltinPath("types.artifact_body_exporter_root")
    record Root() implements ArtifactBodyExporter {}

    @BuiltinFull(uniqueId = 6541L)
    @BuiltinPath("types.artifact_body_exporter_source_ref")
    record SourceRef(ArtifactIdSource id) implements ArtifactBodyExporter {}
}
