package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetInterface(
        Artifact defSet,
        DefSetId defSetId,
        Unique identity,
        Opt<String> description,
        boolean isAspect,
        Opt<Unique> aspectId,
        Opt<BuiltinInterfaceKind> builtinKind,
        String compiledBinaryName,
        Opt<byte[]> compiledBytecode,
        TList<ArtifactDataDefSetInterfaceMethodData> methods)
        implements SafeJsonSerializable {}
