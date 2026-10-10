package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactDataSourceUnion(
        Artifact defSet,
        DefSetId defSetId,
        Unique identity,
        TList<ArtifactDataSourceUnionVariant> variants,
        String baseBinaryName,
        Opt<byte[]> compiledBaseBytecode,
        Opt<TList<byte[]>> compiledVariantBytecode,
        Opt<Long> metaId)
        implements SafeJsonSerializable {}
