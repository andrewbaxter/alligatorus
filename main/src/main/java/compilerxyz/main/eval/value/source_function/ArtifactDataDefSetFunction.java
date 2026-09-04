package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetFunction(
        Artifact defSet,
        DefSetId defSetId,
        Opt<String> name,
        Artifact argType,
        Artifact retType,
        String jvmMethodName,
        Opt<String> compiledClassName,
        Opt<byte[]> compiledBytecode,
        TList<ArtifactExternal> capturedExternals)
        implements SafeJsonSerializable {}
