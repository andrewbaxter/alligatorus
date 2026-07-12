package compilerxyz.eval.value.source_interface;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.Unique;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataDefSetInterface(
    Artifact defSet,
    DefSetId defSetId,
    Unique identity,
    @Nullable String description,
    boolean isAspect,
    @Nullable BuiltinInterfaceKind builtinKind,
    String compiledBinaryName,
    TList<ArtifactDataDefSetInterfaceMethodData> methods)
    implements SafeJsonSerializable {}
