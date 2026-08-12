package compilerxyz.eval.value.source_union;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.Unique;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record ArtifactDataSourceUnion(
    Artifact defSet,
    DefSetId defSetId,
    Unique identity,
    TList<ArtifactDataSourceUnionVariant> variants,
    Opt<String> compiledBaseBinaryName,
    Opt<TList<String>> compiledVariantBinaryNames,
    byte @org.jspecify.annotations.Nullable [] compiledBaseBytecode,
    Opt<TList<byte[]>> compiledVariantBytecode)
    implements SafeJsonSerializable {}
