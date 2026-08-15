package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.Unique;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record ArtifactDataDefSetObject(
    Artifact defSet,
    DefSetId defSetId,
    Opt<String> name,
    Unique identity,
    Artifact underlyingType,
    TList<ArtifactDataDefSetMethod> methods,
    TList<ArtifactDataDefSetInterfaceImpl> interfaceImpls,
    Opt<String> compiledBinaryName,
    Opt<byte[]> compiledBytecode)
    implements SafeJsonSerializable {}
