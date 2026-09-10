package compilerxyz.main.artifact;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6418L)
public sealed interface Artifact {
    record Body(ArtifactBody body) implements Artifact {}

    record Ref(ArtifactIdSource id) implements Artifact {}
}
