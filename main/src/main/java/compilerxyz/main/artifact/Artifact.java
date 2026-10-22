package compilerxyz.main.artifact;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6418L)
@BuiltinPath("types.artifact")
public sealed interface Artifact {
    @BuiltinFull(uniqueId = 6550L)
    @BuiltinPath("types.artifact_inline")
    record Inline(ArtifactBody body) implements Artifact {}

    @BuiltinFull(uniqueId = 6551L)
    @BuiltinPath("types.artifact_ref")
    record Ref(ArtifactIdSource id) implements Artifact {}
}
