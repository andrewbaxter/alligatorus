package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record ArtifactDataDefSetInterfaceImpl(
        Opt<BuiltinInterfaceKind> builtinKind,
        Opt<Artifact> userIface,
        TList<ArtifactDataDefSetInterfaceMethodImpl> methodImpls) {}
