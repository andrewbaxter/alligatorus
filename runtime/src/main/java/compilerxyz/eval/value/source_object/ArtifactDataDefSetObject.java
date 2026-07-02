package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.Unique;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataDefSetObject(
    Artifact defSet,
    DefSetId defSetId,
    @Nullable String name,
    Unique identity,
    String dataField,
    Artifact underlyingType,
    TList<ArtifactDataDefSetMethod> methods,
    TList<ArtifactDataDefSetInterfaceImpl> interfaceImpls,
    @Nullable String compiledBinaryName,
    byte @Nullable [] compiledBytecode)
    implements SafeJsonSerializable {}
