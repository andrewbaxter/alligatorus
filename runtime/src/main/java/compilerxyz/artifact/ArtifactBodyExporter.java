package compilerxyz.artifact;

public sealed interface ArtifactBodyExporter {
    record Body(ArtifactBody inner) implements ArtifactBodyExporter {}

    record Root() implements ArtifactBodyExporter {}

    record SourceRef(ArtifactIdSource id) implements ArtifactBodyExporter {}
}
