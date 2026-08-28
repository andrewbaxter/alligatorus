package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetInterfaceImpl(
        Opt<BuiltinInterfaceKind> builtinKind,
        Opt<Artifact> userIface,
        TList<ArtifactDataDefSetInterfaceMethodImpl> methodImpls) {}
