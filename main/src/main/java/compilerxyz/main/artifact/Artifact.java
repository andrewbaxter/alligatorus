package compilerxyz.main.artifact;

public sealed interface Artifact {
    record Body(ArtifactBody body) implements Artifact {}

    record Ref(ArtifactIdSource id) implements Artifact {}
}
