package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record ArtifactDataDefSetInterfaceImpl(
    @Nullable BuiltinInterfaceKind builtinKind,
    @Nullable Artifact userIface,
    TList<ArtifactDataDefSetInterfaceMethodImpl> methodImpls) {}
