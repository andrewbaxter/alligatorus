package compilerxyz.eval.value.source_interface;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.Unique;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

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
