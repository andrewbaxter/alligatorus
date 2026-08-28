package compilerxyz.eval.value.source_function;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.DefSetId;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record ArtifactDataDefSetFunction(
        Artifact defSet,
        DefSetId defSetId,
        Opt<String> name,
        Artifact argType,
        Artifact retType,
        Opt<String> compiledClassName,
        Opt<byte[]> compiledBytecode,
        TList<ArtifactExternal> capturedExternals)
        implements SafeJsonSerializable {}
