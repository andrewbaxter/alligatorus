package compilerxyz.eval.value.source_union;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.Unique;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataSourceUnion(
    Artifact defSet,
    DefSetId defSetId,
    Unique identity,
    TList<ArtifactDataSourceUnionVariant> variants,
    @Nullable String compiledBaseBinaryName,
    @Nullable TList<String> compiledVariantBinaryNames,
    byte @Nullable [] compiledBaseBytecode,
    @Nullable TList<byte[]> compiledVariantBytecode)
    implements SafeJsonSerializable {}
